package ar.edu.itba.ss.gasdiffusion.core;

import ar.edu.itba.ss.gasdiffusion.models.Point;
import ar.edu.itba.ss.gasdiffusion.models.Wall;
import ar.edu.itba.ss.gasdiffusion.services.PointFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static ar.edu.itba.ss.gasdiffusion.core.Main.EXIT_CODE.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Main {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  private static final String DESTINATION_FOLDER = "output";
  private static final String STATIC_FILE = "static.dat";
  private static final String DYNAMIC_FILE = "dynamic.dat";
  private static final String OUTPUT_FILE = "output.dat";
  private static final String VA_FILE = "t_va.csv";
  private static final String OVITO_FILE = "graphics.xyz";
  private static final String HELP_TEXT =
          "Gas Diffusion 2D Simulation Implementation.\n" +
                  "Arguments: \n" +
                  "* gen staticdat <N> <m> <v> <r> <L> <W>  : \n" +
                  "\t generates an output/static.dat file of N particles of radio r\n" +
                  "\t that will be contained on a rectangle of height L and width W. All particles will move at a speed of v\n" +
                  "* gen dynamicdat <path/to/static.dat> : \n" +
                  "\t generates an output/dynamic.dat file of N particles, \n" +
                  "\t each of the specified radio, that have x & y coordinates\n" +
                  "\t between 0 (inclusive) and W/2 (exclusive) for the x coordinate and between 0 (inclusive) and L for the y coordinate.\n" +
                  "\t Particles will also have an orientation between 0 and 2*PI\n" +
                  "* gas <path/to/static.dat> <path/to/dynamic.dat> <dt2> <opening>\n" +
                  "\t runs the gas-diffusion simulation and saves a snapshot of the system every dt2 time in <output.dat>.\n" +
                  "* gen ovito <path/to/static.dat> <path/to/output.dat> : \n"+
                  "\t generates an output/graphics.xyz file (for Ovito) with the result of the gas diffusion\n " +
                  "\t automaton(<output.dat>) generated with the other two files.\n";

  // Exit Codes
  enum EXIT_CODE {
    NO_ARGS(-1),
    NO_FILE(-2),
    BAD_N_ARGUMENTS(-3),
    BAD_ARGUMENT(-4),
    NOT_A_FILE(-5),
    UNEXPECTED_ERROR(-6),
    BAD_FILE_FORMAT(-7);

    private final int code;

    EXIT_CODE(final int code) {
      this.code = code;
    }

    public int getCode() {
      return code;
    }
  }

  private static void exit(final EXIT_CODE exitCode) {
    System.exit(exitCode.getCode());
  }

  public static void main(final String[] args) {
    if (args.length == 0) {
      System.out.println("[FAIL] - No arguments passed. Try 'help' for more information.");
      exit(NO_ARGS);
    }

    switch (args[0]) {
      case "help":
        System.out.println(HELP_TEXT);
        break;
      case "gen":
        generateCase(args);
        break;
      case "gas":
        gasDiffusion(args);
        break;
      default:
        System.out.println("[FAIL] - Invalid argument. Try 'help' for more information.");
        exit(BAD_ARGUMENT);
        break;
    }

    System.out.println("[DONE]");
  }

  private static void gasDiffusion(final String[] args) {
    if (args.length != 5) {
      System.out.println("[FAIL] - Bad number of arguments. Try 'help' for more information.");
      exit(BAD_N_ARGUMENTS);
    }

    // create points' set with static and dynamic files
    final StaticData staticData = loadStaticFile(args[1]);

    List<Point> points = new ArrayList<>(loadDynamicFile(args[2], staticData));

    double dt2 = 0;
    try {
      dt2 = Double.parseDouble(args[3]);
    } catch (NumberFormatException e) {
      LOGGER.warn("[FAIL] - <dt2> must be a number. Caused by: ", e);
      System.out.println("[FAIL] - <dt2> argument must be a number. Try 'help' for more information.");
      exit(BAD_ARGUMENT);
    }

    double opening = 0;
    try {
      opening = Double.parseDouble(args[4]);
    } catch (NumberFormatException e) {
      LOGGER.warn("[FAIL] - <opening> must be a number. Caused by: ", e);
      System.out.println("[FAIL] - <opening> argument must be a number. Try 'help' for more information.");
      exit(BAD_ARGUMENT);
    }

    if(staticData.mass <= 0
            || staticData.L <= 0
            || opening <= 0 || opening > staticData.L
            || staticData.W <= 0
            || dt2 < 0) {
      System.out.println("[FAIL] - The following must not happen: mass < 0 or L <= 0 or opening <= 0 or opening > L or W <= 0.\n" +
              "Please check the input files.");
      exit(BAD_ARGUMENT);
    }

    // Create file for first iteration
    final File dataFolder = new File(DESTINATION_FOLDER);
    dataFolder.mkdirs(); // tries to make directories for the .dat files

    /* delete previous dynamic.dat file, if any */
    final Path pathToDatFile = Paths.get(DESTINATION_FOLDER, OUTPUT_FILE);
    final Path pathToVaFile = Paths.get(DESTINATION_FOLDER, VA_FILE);

    if(!deleteIfExists(pathToDatFile)) {
      return;
    }
    if(!deleteIfExists(pathToVaFile)) {
      return;
    }
    generateOutputDatFile(points, 0);

    Wall.HORIZONTAL.setLength(staticData.W);
    Wall.VERTICAL.setLength(staticData.L);

    final GasDiffusion gasDiffusion = new GasDiffusion(staticData.L, staticData.W, opening);

    double fraction = 1.0, currentTime = 0;
    long maxIterations = 100;

    for(long i = 0; i < maxIterations /* && fraction > 0.5 */; i++) { //TODO: Finish by fractionParticles and not by number of iterations
      points = gasDiffusion.run(points);
      currentTime += gasDiffusion.getCollisionTime(); // Time left to reach dt2

      if (currentTime >= dt2) {
        currentTime = 0.0; // reset time counter
        generateOutputDatFile(points, i); // save to file the current configuration
      }
      fraction = gasDiffusion.getFraction();
    }


  }

  /**
   * Format:  ID X Y Vx Vy R G B
   * @param updatedParticles set of particles to be persisted
   * @param iteration the iteration number
   */
  private static void generateOutputDatFile(final Collection<Point> updatedParticles, final long iteration) {
    /* delete previous dynamic.dat file, if any */
    final Path pathToDatFile = Paths.get(DESTINATION_FOLDER, OUTPUT_FILE);
    final Path pathToVaFile = Paths.get(DESTINATION_FOLDER, VA_FILE);

    /* write the new output.dat file */
    final String[] data = pointsToString(updatedParticles, iteration);

    BufferedWriter writer = null;
    BufferedWriter va_writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(pathToDatFile.toFile(), true));
      writer.write(data[0]);

      va_writer = new BufferedWriter(new FileWriter(pathToVaFile.toFile(), true));
      va_writer.write(data[1]); // write va data
      va_writer.write("\n");

    } catch (IOException e) {
      LOGGER.warn("An unexpected IO Exception occurred while writing the file {}. Caused by: ", pathToDatFile, e);
      System.out.println("[FAIL] - An unexpected error occurred while writing the file '" + pathToDatFile + "'. \n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(UNEXPECTED_ERROR);
    } finally {
      try {
        // close the writer regardless of what happens...
        if (writer != null) {
          writer.close();
        }
        if (va_writer != null) {
          va_writer.close();
        }
      } catch (Exception ignored) {

      }
    }
  }

  private static void generateCase(final String[] args) {
    // another arg is needed
    if (args.length < 2) {
      System.out.println("[FAIL] - No file specified. Try 'help' for more information.");
      exit(NO_FILE);
    }

    switch (args[1]) {
      case "staticdat":
        if (args.length != 8) {
          System.out.println("[FAIL] - Bad number of arguments. Try 'help' for more information.");
          exit(BAD_N_ARGUMENTS);
        }

        int N = 0;
        try {
          N = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <N> must be a positive integer. Caused by: ", e);
          System.out.println("[FAIL] - <N> must be a positive integer. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        double mass = 0;
        try {
          mass = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <m> must be a number. Caused by: ", e);
          System.out.println("[FAIL] - <m> argument must be a number. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        double speed = 0;
        try {
          speed = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <speed> must be a number. Caused by: ", e);
          System.out.println("[FAIL] - <speed> argument must be a number. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        double r = 0;
        try {
          r = Double.parseDouble(args[5]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <r> must be a number. Caused by: ", e);
          System.out.println("[FAIL] - <r> argument must be a number. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        double L = 0;
        try {
          L = Double.parseDouble(args[6]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <L> must be a number. Caused by: ", e);
          System.out.println("[FAIL] - <L> argument must be a number. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        double W = 0;
        try {
          W = Double.parseDouble(args[7]);
        } catch (NumberFormatException e) {
          LOGGER.warn("[FAIL] - <M> must be a number. Caused by: ", e);
          System.out.println("[FAIL] - <M> argument must be a number. Try 'help' for more information.");
          exit(BAD_ARGUMENT);
        }

        // create the points position, given the static.dat file
        generateStaticDatFile(N, mass, speed, r, L, W);

        break;
      case "dynamicdat":
        if (args.length != 3) {
          System.out.println("[FAIL] - Bad number of arguments. Try 'help' for more information.");
          exit(BAD_N_ARGUMENTS);
        }

        // read N, L and rs from an input file
        final StaticData staticData = loadStaticFile(args[2]);

        // create the points position, given the static.dat file
        generateDynamicDatFile(staticData);
        break;

      case "ovito":
        // get particle id
        if (args.length != 4) {
          System.out.println("[FAIL] - Bad number of arguments. Try 'help' for more information.");
          exit(BAD_N_ARGUMENTS);
        }

        final String staticFile = args[2];
        final String outputFile = args[3];

        generateOvitoFile(staticFile, outputFile);
        break;

      default:
        System.out.println("[FAIL] - Invalid argument. Try 'help' for more information.");
        exit(BAD_ARGUMENT);
        break;
    }
  }

  private static void generateStaticDatFile(final int N, final double mass, final double speed, final double r,
                                            final double L, final double W) {
    // save data to a new file
    final File dataFolder = new File(DESTINATION_FOLDER);
    dataFolder.mkdirs(); // tries to make directories for the .dat files

    /* delete previous dynamic.dat file, if any */
    final Path pathToDatFile = Paths.get(DESTINATION_FOLDER, STATIC_FILE);

    if(!deleteIfExists(pathToDatFile)) {
      return;
    }

    /* write the new static.dat file */
    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(pathToDatFile.toFile()));
      writer.write(String.valueOf(N));
      writer.write("\n");
      writer.write(String.valueOf(mass));
      writer.write("\n");
      writer.write(String.valueOf(speed));
      writer.write("\n");
      final String radio = String.valueOf(r);
      for (int i = 0 ; i < N ; i++) {
        writer.write(radio);
        writer.write("\n");
      }
      writer.write(String.valueOf(L));
      writer.write("\n");
      writer.write(String.valueOf(W));
      writer.write("\n");

    } catch (IOException e) {
      LOGGER.warn("An unexpected IO Exception occurred while writing the file {}. Caused by: ", pathToDatFile, e);
      System.out.println("[FAIL] - An unexpected error occurred while writing the file '" + pathToDatFile + "'. \n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(UNEXPECTED_ERROR);
    } finally {
      try {
        // close the writer regardless of what happens...
        if (writer != null) {
          writer.close();
        }
      } catch (Exception ignored) {

      }
    }
  }

  private static void generateDynamicDatFile(final StaticData staticData) {
    final PointFactory pF = PointFactory.getInstance();

    final Point leftBottomPoint = Point.builder(0, 0).vx(0).vy(0).build();
    //final Point rightTopPoint = Point.builder(staticData.L, staticData.W / 2).speed(0).orientation(0).build();
    final Point rightTopPoint = Point.builder(staticData.W / 2, staticData.L).vx(0).vy(0).build();

    final Set<Point> pointsSet = pF.randomPoints(leftBottomPoint, rightTopPoint,
            staticData.radios, false, Integer.MAX_VALUE, staticData.speed, staticData.mass);

    if (pointsSet.size() < staticData.radios.length) {
      System.out.println("[FAIL] - Could not generate all the particles from the static file.\n" +
              "They where crashing each other when trying to create them at different positions.\n" +
              "Check that N is not that big for the given L and W.\n" +
              "Aborting...");
      exit(UNEXPECTED_ERROR);
    }

    // save data to a new file
    final File dataFolder = new File(DESTINATION_FOLDER);
    dataFolder.mkdirs(); // tries to make directories for the .dat files

    /* delete previous dynamic.dat file, if any */
    final Path pathToDatFile = Paths.get(DESTINATION_FOLDER, DYNAMIC_FILE);

    if(!deleteIfExists(pathToDatFile)) {
      return;
    }

    /* write the new dynamic.dat file */
    final String pointsAsFileFormat = pointsToString(pointsSet);

    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(pathToDatFile.toFile()));
      writer.write(pointsAsFileFormat);
    } catch (IOException e) {
      LOGGER.warn("An unexpected IO Exception occurred while writing the file {}. Caused by: ", pathToDatFile, e);
      System.out.println("[FAIL] - An unexpected error occurred while writing the file '" + pathToDatFile + "'. \n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(UNEXPECTED_ERROR);
    } finally {
      try {
        // close the writer regardless of what happens...
        if (writer != null) {
          writer.close();
        }
      } catch (Exception ignored) {

      }
    }
  }

  // Used for building dynamic file
  private static String pointsToString(final Set<Point> pointsSet) {
    final StringBuffer sb = new StringBuffer();
    sb.append(0).append('\n');
    pointsSet.forEach(point -> sb
            .append(point.x()).append('\t')
            .append(point.y()).append('\t')
            .append(point.vx()).append('\t')
            .append(point.vy()).append('\t')
            .append('\n'));
    return sb.toString();
  }
  // Used for building output.dat
  private static String[] pointsToString(final Collection<Point> pointsSet, final long iteration) {
    final StringBuilder sb = new StringBuilder();
    sb.append(iteration).append('\n');
    double vx, vy, r, g, b;
    double vax = 0;
    double vay = 0;
    double v = 0;
    double orientation;

    for (final Point point : pointsSet) {
      vx = point.vx();
      vy = point.vy();
      orientation = Math.tan(vy / vx);
      vax += vx;
      vay += vy;
      v += point.speed();
      r = Math.cos(orientation);
      g = Math.sin(orientation);
      b = Math.cos(orientation) * Math.sin(orientation);
      sb.append(point.id()).append('\t')
              // position
              .append(point.x()).append('\t').append(point.y()).append('\t')
              // velocity
              .append(vx).append('\t').append(vy).append('\t')
              // R G B colors
              .append(r).append('\t')
              .append(g).append('\t')
              .append(b).append('\n');
    }

    // calculate the current va, assuming the average of all point's speeds (works for the current case)
    // 1/(N * v/N) = 1/v for this case, assuming the above is valid

    final double va;
    if (!pointsSet.isEmpty()) {
      va = (1/v) * (Math.sqrt(Math.pow(vax,2) + Math.pow(vay,2)));
    } else {
      va = -1;
    }

    final String[] answer = new String[2];
    answer[0] = sb.toString();
    answer[1] = String.valueOf(va);

    return answer;
  }

  /**
   *  Generate a .XYZ file which contains the following information about a particle:
   *  - id
   *  - X Position
   *  - Y Position
   *  - X Speed
   *  - Y Speed
   *  - R color - vx
   *  - G color - vy
   *  - B color - vx + vy
   *  By default, the output file is 'graphics.xyz' which is stored in the 'data' folder.
   * @param staticFile -
   * @param outputFile -
   */
  private static void generateOvitoFile(final String staticFile, final String outputFile) {
    final Path pathToStaticDatFile = Paths.get(staticFile);
    final Path pathToOutputDatFile = Paths.get(outputFile);
    final Path pathToGraphicsFile = Paths.get(DESTINATION_FOLDER, OVITO_FILE);

    // save data to a new file
    final File dataFolder = new File(DESTINATION_FOLDER);
    //noinspection ResultOfMethodCallIgnored
    dataFolder.mkdirs(); // tries to make directories for the .dat files

    /* delete previous dynamic.dat file, if any */
    if(!deleteIfExists(pathToGraphicsFile)) {
      return;
    }

    Stream<String> staticDatStream = null;
    Stream<String> outputDatStream = null;

    try {
      staticDatStream = Files.lines(pathToStaticDatFile);
      outputDatStream = Files.lines(pathToOutputDatFile);
    } catch (IOException e) {
      LOGGER.warn("Could not read a file. Details: ", e);
      System.out.println("Could not read one of these files: '" + pathToStaticDatFile + "' or '"
              + pathToOutputDatFile + "'.\n" +
              "Check the logs for a detailed info.\n" +
              "Aborting...");
      exit(UNEXPECTED_ERROR);
    }

    BufferedWriter writer = null;

    try {
      String stringN; // N as string
      String iterationNum, borderParticles;
      int N;
      final double L, W;
      final Iterator<String> staticDatIterator;
      final Iterator<String> outputDatIterator;
      final StringBuilder sb = new StringBuilder();

      final StaticData staticData = loadStaticFile(staticFile);

      writer = new BufferedWriter(new FileWriter(pathToGraphicsFile.toFile()));
      staticDatIterator = staticDatStream.iterator();
      outputDatIterator = outputDatStream.iterator();

      // Write number of particles
      stringN = staticDatIterator.next();
      N = Integer.valueOf(stringN);

      //L = Double.valueOf(staticDatIterator.next());
      //W = Double.valueOf(staticDatIterator.next());
      L = staticData.L;
      W = staticData.W;

      // Create virtual particles in the borders, in order for Ovito to show the whole board
      sb.append(N+1).append('\t').append(0).append('\t').append(0).append('\t').append(0)
              .append('\t').append(0).append('\t')
              // color: black
              .append(0).append('\t').append(0).append('\t').append(0)
              .append('\n');
      sb.append(N+2).append('\t').append(0).append('\t').append(L).append('\t').append(0)
              .append('\t').append(0).append('\t')
              // color: black
              .append(0).append('\t').append(0).append('\t').append(0)
              .append('\n');
      sb.append(N+3).append('\t').append(W).append('\t').append(0).append('\t').append(0)
              .append('\t').append(0).append('\t')
              // color: black
              .append(0).append('\t').append(0).append('\t').append(0)
              .append('\n');
      sb.append(N+4).append('\t').append(W).append('\t').append(L).append('\t').append(0)
              .append('\t').append(0).append('\t')
              // color: black
              .append(0).append('\t').append(0).append('\t').append(0)
              .append('\n');
      sb.append(N+5).append('\t').append(W/2).append('\t').append(0).append('\t').append(0)
              .append('\t').append(0).append('\t')
              // color: black
              .append(0).append('\t').append(0).append('\t').append(0)
              .append('\n');
      sb.append(N+6).append('\t').append(W/2).append('\t').append(L).append('\t').append(0)
              .append('\t').append(0).append('\t')
              // color: black
              .append(0).append('\t').append(0).append('\t').append(0)
              .append('\n');

      stringN = String.valueOf(N+6);

      borderParticles = sb.toString();

      while(outputDatIterator.hasNext()){
        // Write ammount of particles (N)
        writer.write(stringN);
        writer.newLine();

        // Write iteration number
        iterationNum = outputDatIterator.next();
        writer.write(iterationNum);
        writer.newLine();

                /*
                  Write particle information in this order
                  Particle_Id     X_Pos	Y_Pos   X_Vel   Y_Vel R G B
                */
        for(int i=0; i<N; i++){
          writer.write(outputDatIterator.next() + "\n");
        }

        // Write border particles
        writer.write(borderParticles);


      }
    } catch(final IOException e) {
      LOGGER.warn("Could not write to '{}'. Caused by: ", pathToGraphicsFile, e);
      System.out.println("Could not write to '" + pathToGraphicsFile + "'." +
              "\nCheck the logs for a detailed info.\n" +
              "Aborting...");
      exit(UNEXPECTED_ERROR);
    } finally {
      try {
        if(writer != null) {
          writer.close();
        }
        staticDatStream.close();
        outputDatStream.close();
      } catch (final IOException ignored) {

      }
    }
  }

  /**
   * Try to delete a file, whether it exists or not
   * @param pathToFile the file path that refers to the file that will be deleted
   * @return true if there were not errors when trying to delete the file;
   * 		   false in other case;
   */
  private static boolean deleteIfExists(final Path pathToFile) {
    try {
      Files.deleteIfExists(pathToFile);
    } catch(IOException e) {
      LOGGER.warn("Could not delete previous file: '{}'. Caused by: ", pathToFile, e);
      System.out.println("Could not delete previous file: '" + pathToFile + "'.\n");
      return false;
    }
    return true;
  }

  /* ------------------------------- */
  private static class StaticData {
    private int N;
    private double mass;
    private double[] radios;
    private double speed;
    private double L;
    private double W;
  }

  private static StaticData loadStaticFile(final String filePath) {
    final StaticData staticData = new StaticData();

    final File staticFile = new File(filePath);
    if (!staticFile.isFile()) {
      System.out.println("[FAIL] - File '" + filePath + "' is not a normal file. Aborting...");
      exit(NOT_A_FILE);
    }

    try (final Stream<String> staticStream = Files.lines(staticFile.toPath())) {
      final Iterator<String> staticFileLines = staticStream.iterator();

      // get N
      staticData.N = Integer.valueOf(staticFileLines.next());

      // get mass
      staticData.mass = Double.valueOf(staticFileLines.next());

      // get speed
      staticData.speed = Double.valueOf(staticFileLines.next());

      // get radios
      staticData.radios = new double[staticData.N];
      String cLine;
      double cRadio;
      for (int i = 0 ; i < staticData.N ; i++) {
        cLine = staticFileLines.next(); // caught runtime exception
        cRadio = Double.valueOf(cLine.split(" ")[0]); // at least it should have one component
        staticData.radios[i] = cRadio;
      }

      // get height
      staticData.L = Double.valueOf(staticFileLines.next());

      // get Width
      staticData.W = Double.valueOf(staticFileLines.next());

    } catch (final IOException e) {
      LOGGER.warn("An unexpected IO Exception occurred while reading the file {}. Caused by: ", staticFile, e);
      System.out.println("[FAIL] - An unexpected error occurred while reading the file '" + staticFile + "'. \n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(UNEXPECTED_ERROR);
    } catch (final NumberFormatException e) {
      LOGGER.warn("[FAIL] - Number expected. Caused by: ", e);
      System.out.println("[FAIL] - Bad format of file '" + staticFile + "'.\n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(BAD_FILE_FORMAT);
    } catch (final NoSuchElementException e) {
      LOGGER.warn("[FAIL] - Particle Expected. Caused by: ", e);
      System.out.println("[FAIL] - Bad format of file '" + staticFile + "'.\n" +
              "Particle information expected: N is greater than the # of lines with particle information.\n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(BAD_FILE_FORMAT);
    } catch (NegativeArraySizeException e) {
      LOGGER.warn("[FAIL] - N positive expected. Caused by: ", e);
      System.out.println("[FAIL] - Bad format of file '" + staticFile + "'.\n" +
              "Particle information expected: N is lower than 0\n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(BAD_FILE_FORMAT);
    }

    return staticData;
  }

  private static Set<Point> loadDynamicFile(final String fileName, final StaticData staticData) {
    final File dynamicFile = new File(fileName);
    if (!dynamicFile.isFile()) {
      System.out.println("[FAIL] - File '" + fileName + "' is not a normal file. Aborting...");
      exit(NOT_A_FILE);
    }


    final Set<Point> points = new HashSet<>(staticData.radios.length);

    try (final Stream<String> dynamicStream = Files.lines(dynamicFile.toPath())) {
      final Iterator<String> dynamicFileLines = dynamicStream.iterator();

      // skip time t0
      dynamicFileLines.next();

      double x, y, vx, vy;
      for (int i = 0 ; i < staticData.radios.length ; i++) {
        final Scanner intScanner = new Scanner(dynamicFileLines.next());
        x = intScanner.nextDouble(); // caught InputMismatchException
        y = intScanner.nextDouble(); // caught InputMismatchException
        vx = intScanner.nextDouble();
        vy = intScanner.nextDouble();
        points.add(Point.builder(x,y).radio(staticData.radios[i]).vx(vx).vy(vy).mass(staticData.mass).build());
      }

    } catch (IOException e) {
      LOGGER.warn("An unexpected IO Exception occurred while reading the file {}. Caused by: ", dynamicFile, e);
      System.out.println("[FAIL] - An unexpected error occurred while reading the file '" + dynamicFile + "'. \n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(UNEXPECTED_ERROR);
    } catch (final InputMismatchException e) {
      LOGGER.warn("[FAIL] - Number expected. Caused by: ", e);
      System.out.println("[FAIL] - Bad format of file '" + dynamicFile + "'.\n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(BAD_FILE_FORMAT);
    } catch (final NoSuchElementException e) {
      LOGGER.warn("[FAIL] - Particle Expected. Caused by: ", e);
      System.out.println("[FAIL] - Bad format of file '" + dynamicFile + "'.\n" +
              "Particle information expected: N is greater than the # of lines with particle information.\n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(BAD_FILE_FORMAT);
    } catch (final IndexOutOfBoundsException e) {
      LOGGER.warn("[FAIL] - Particle Information Missing. Caused by: ", e);
      System.out.println("[FAIL] - Bad format of file '" + dynamicFile + "'.\n" +
              "Particle information missing: x or y position is missing.\n" +
              "Check the logs for more info.\n" +
              "Aborting...");
      exit(BAD_FILE_FORMAT);
    }

    return points;
  }
}
