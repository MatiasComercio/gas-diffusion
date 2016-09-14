# Gas Diffusion 2D
Java Implementation of the Gas Diffusion Simulation
## Build
To build the project, it is necessary to have Maven and Java 1.8 installed.
Then, run

    $ mvn clean package
    
## Execution
To run the program, from the root folder

    $ java -jar core/target/gas-diffusion.jar <arguments>
    
## Simulation
Script file was added to `resources/bin` folder.

Please open this file and read the description at their top to check which variables are required and what do they mean.
Simulation's output folder will be at the `output` folder from the main project's folder.

**Note that there is no need to set up any variable.**

### Parameters
- `$ ./gas-diffusion.sh` - This script requires 8 parameters: `<N> <m> <v> <r> <L> <W> <dt2> <opening>`
- `$ ./analyser.sh` - This script requires 0 or 6 parameters: `<m> <v> <r> <L> <W> <dt2> <cIterations>`

###Usage example
Run the `gas-diffusion` script as follows:

    $ ./gas-diffusion.sh 100 1 .01 .0015 0.09 0.24 0.04 .006

Run the `analyser` script as follows:

    $ ./analyser.sh