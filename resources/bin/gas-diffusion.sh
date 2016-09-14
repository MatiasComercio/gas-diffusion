#!/bin/bash

# Gas Diffusion 2D Simulation Implementation.
# Arguments:
# * gen staticdat <N> <m> <v> <r> <L> <W>  :
#    generates an output/static.dat file of N particles of radio r
#    that will be contained on a rectangle of height L and width W. All particles will move at a speed of v
# * gen dynamicdat <path/to/static.dat> :
#    generates an output/dynamic.dat file of N particles,
#    each of the specified radio, that have x & y coordinates
#    between 0 (inclusive) and W/2 (exclusive) for the x coordinate and between 0 (inclusive) and L for the y coordinate.
#    Particles will also have an orientation between 0 and 2*PI
# * gas <path/to/static.dat> <path/to/dynamic.dat> <dt2> <opening>
#    runs the gas-diffusion simulation and saves a snapshot of the system every dt2 time in <output.dat>.
# * gen ovito <path/to/static.dat> <path/to/output.dat> :
#    generates an output/graphics.xyz file (for Ovito) with the result of the gas diffusion
#     automaton(<output.dat>) generated with the other two files.
#
# Usage of this bash:
# ./gas-diffusion.sh <N> <m> <v> <r> <L> <W> <dt2> <opening>


# Paths to required files. Source:
# http://stackoverflow.com/questions/630372/determine-the-path-of-the-executing-bash-script#answer-630387
CURR="`dirname \"$0\"`" # relative
# enter the relative folder from where the script is running ang get the pwd
BIN_FOLDER="`( cd \"$CURR\" && pwd )`"  # absolutized and normalized
if [ -z "$BIN_FOLDER" ] ; then
  # error; for some reason, the path is not accessible
  # to the script (e.g. permissions re-evaled after suid)
  echo "[FAIL] - Unexpected error. Aborting..."
  exit 1  # fail
fi
PROJECT_FOLDER="`cd $BIN_FOLDER/../.. && pwd`"
if [ -z "$PROJECT_FOLDER" ] ; then
  # error; for some reason, the path is not accessible
  # to the script (e.g. permissions re-evaled after suid)
  echo "[FAIL] - Unexpected error. Aborting..."
  exit 1  # fail
fi

OUTPUT_FOLDER="$BIN_FOLDER/output"
STATIC_PATH="$OUTPUT_FOLDER/static.dat"
DYNAMIC_PATH="$OUTPUT_FOLDER/dynamic.dat"
SIM_OUTPUT_PATH="$OUTPUT_FOLDER/output.dat"
OVITO_OUTPUT_PATH="$OUTPUT_FOLDER/graphics.xyz"

JAR="java -jar $PROJECT_FOLDER/core/target/gas-diffusion.jar"

# Number of exact parameters required to run the script
PARAMS_REQUIRED=8

# Start of Script
START_TIME=$(date +%s)

if [ $# -ne ${PARAMS_REQUIRED} ]; then
  echo "[FAIL] - This script requires $PARAMS_REQUIRED parameters: <N> <m> <v> <r> <L> <W> <dt2> <opening>"
  exit 1
fi

# Assign arguments to readable variables
N=$1
m=$2
v=$3
r=$4
L=$5
W=$6
dt2=$7
opening=$8

# Generate static.dat
# * gen staticdat <N> <m> <v> <r> <L> <W>  :
#    generates an output/static.dat file of N particles of radio r
#    that will be contained on a rectangle of height L and width W. All particles will move at a speed of v
echo -e "Generating static.dat... "
${JAR} gen staticdat ${N} ${m} ${v} ${r} ${L} ${W}

# Generate dynamic.dat
# * gen dynamicdat <path/to/static.dat> :
#    generates an output/dynamic.dat file of N particles,
#    each of the specified radio, that have x & y coordinates
#    between 0 (inclusive) and W/2 (exclusive) for the x coordinate and between 0 (inclusive) and L for the y coordinate.
echo -e "Generating dynamic.dat... "
${JAR} gen dynamicdat ${STATIC_PATH}

# Generate output.dat
# * gas <path/to/static.dat> <path/to/dynamic.dat> <dt2> <opening>
#    runs the gas-diffusion simulation and saves a snapshot of the system every dt2 time in <output.dat>.
echo -e "Generating output.dat... "
${JAR} gas ${STATIC_PATH} ${DYNAMIC_PATH} ${dt2} ${opening}

# Generate graphics.xyz
# * gen ovito <path/to/static.dat> <path/to/output.dat> :
#    generates an output/graphics.xyz file (for Ovito) with the result of the gas diffusion
#     automaton(<output.dat>) generated with the other two files.
echo -e "Generating graphics.xyz... "
${JAR} gen ovito ${STATIC_PATH} ${SIM_OUTPUT_PATH} ${opening}

END_TIME=$(date +%s)

EXECUTION_TIME=`expr $END_TIME - $START_TIME`
EXECUTION_TIME_FILE="${OUTPUT_FOLDER}/execution_time.statistics"
rm -f ${EXECUTION_TIME_FILE}
touch ${EXECUTION_TIME_FILE}
echo -e "Execution time: ${EXECUTION_TIME} seconds\r" >> ${EXECUTION_TIME_FILE}
echo -e "Execution time: ${EXECUTION_TIME} seconds"

# Move output folder to parent project's folder
DATE_TIME=$(date +%Y-%m-%d-%H%M%S)
FINAL_OUTPUT_FOLDER=${PROJECT_FOLDER}/output/gas-diffusion/${DATE_TIME}
mkdir -p ${FINAL_OUTPUT_FOLDER}
mv ${OUTPUT_FOLDER}/* ${FINAL_OUTPUT_FOLDER}
rm -rf ${OUTPUT_FOLDER}

# Move log folder to parent project's folder
mkdir -p ${FINAL_OUTPUT_FOLDER}/logs
mv logs/* ${FINAL_OUTPUT_FOLDER}/logs
rm -rf logs
