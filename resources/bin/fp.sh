#!/bin/bash

# Usage of this bash:
# ./analyser.sh <m> <r> <L> <W> <dt2> <cIterations>
# ./analyser.sh, which takes the default values
#
# Analyses the behaviour of different systems that vary only on the <N> value (number of particles)
# and generates statistics about time to reach the equilibrium (both sides of the box with
# equal fraction of particles), and the relation between pressure and temperature, analysed among
# different systems that vary only on the number of particles (as it has been said)

## Constants

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
SIM_I_T_FP_PRE_TEMP_PATH="$OUTPUT_FOLDER/i_t_fp_pre_temp.csv"
SIM_TIME_TO_EQ_PATH="$OUTPUT_FOLDER/time_to_eq.csv"
OVITO_OUTPUT_PATH="$OUTPUT_FOLDER/graphics.xyz"
SCRIPT_LOGGER="$OUTPUT_FOLDER/analysis.sh.log"

RESULTS_FOLDER="$OUTPUT_FOLDER/results"

# i_t_fp_pre_temp_NXXX_OPXXX.csv
N_OP_RESULTS="$RESULTS_FOLDER/i_t_fp_pre_temp_"

# pre_t_results_NXXX_OPXXX.csv
PRE_T_RESULTS="$RESULTS_FOLDER/pre_t_results_"

JAR="java -jar $PROJECT_FOLDER/core/target/gas-diffusion.jar"

# Extension of the output table
OUTPUT_TABLE_TYPE=".csv"

########################
## Functions

# Generate static.dat
function gen_static {
    ${JAR} gen staticdat ${N} ${m} ${v} ${r} ${L} ${W} >> ${SCRIPT_LOGGER} 2>&1
}

# Generate dynamic.dat
function gen_dynamic {
    ${JAR} gen dynamicdat ${STATIC_PATH} >> ${SCRIPT_LOGGER} 2>&1
}

# Generate output.dat
function gen_output {
    ${JAR} gas ${STATIC_PATH} ${DYNAMIC_PATH} ${dt2} ${OP} >> ${SCRIPT_LOGGER} 2>&1
}

function validate_exit_status {
    if [ $? -ne 0 ]
    then
        echo "[FAIL] - check ${SCRIPT_LOGGER} file for more information"
        echo "Simulation will continue although, but problems may arise due to the previous one"
    else
        echo "[DONE]"
    fi
}

function write_constants {
  echo -e "N, ${N}\r" >> ${OUTPUT_TABLE_PATH}
  echo -e "opening, ${OP}\r" >> ${OUTPUT_TABLE_PATH}
  echo -e "v, ${v}\r" >> ${OUTPUT_TABLE_PATH}
  echo -e "m, ${m}\r" >> ${OUTPUT_TABLE_PATH}
  echo -e "r, ${r}\r" >> ${OUTPUT_TABLE_PATH}
  echo -e "L, ${L}\r" >> ${OUTPUT_TABLE_PATH}
  echo -e "W, ${W}\r" >> ${OUTPUT_TABLE_PATH}
  echo -e "dt2, ${dt2}\r" >> ${OUTPUT_TABLE_PATH}
}

function gen_i_t_fp {
    local OUTPUT_TABLE_PREFIX

    local OUTPUT_TABLE_PATH="${N_OP_RESULTS}V${v}_N${N}_OP${OP}$OUTPUT_TABLE_TYPE"

    # Delete and create output table file
    rm -f ${OUTPUT_TABLE_PATH}
    touch ${OUTPUT_TABLE_PATH}

    # write constants L number to the file
    write_constants

    echo ${OUTPUT_TABLE_PATH}
}

function gen_pre_t_results {
    local OUTPUT_TABLE_PREFIX

    # e.g. "va_noise_all_NXXX.csv"
    local OUTPUT_TABLE_PATH="${PRE_T_RESULTS}N${N}_OP${OP}$OUTPUT_TABLE_TYPE"

    # Delete and create output table file
    rm -f ${OUTPUT_TABLE_PATH}
    touch ${OUTPUT_TABLE_PATH}

    write_constants

    # Add identifiers of columns to the start of the file
    echo -e "V, Temperature, E(Pressure) = |Pressure| = mean(Pressure), "\
    "SD(Pressure) = sqrt(|Pressure^2|-|Pressure|^2)\r\n" >> ${OUTPUT_TABLE_PATH}

    echo ${OUTPUT_TABLE_PATH}
}

# Possible calls:
# ./analyser.sh <m> <r> <L> <W> <dt2> <cIterations>
# ./analyser.sh
PARAMS_REQUIRED=6

#################

# Start of Script
START_TIME=$(date +%s)
DATE_TIME=$(date +%Y-%m-%d-%H%M%S)
echo -e "Start Time: ${DATE_TIME}"

if [ $# -eq ${PARAMS_REQUIRED} ] ; then
  m=$1
  r=$2
  L=$3
  W=$4
  dt2=$5
  C_ITERATIONS=$6
elif [ $# -eq 0 ]; then
  m=1
  r=0.0015
  L=0.09
  W=0.24
  dt2=0.1
  C_ITERATIONS=50
else
  echo "[FAIL] - This script requires 0 or $PARAMS_REQUIRED parameters - <m> <v> <r> <L> <W> <dt2> <cIterations>"
  echo "Aborting..."
  exit 1
fi

# create results folder
mkdir -p ${RESULTS_FOLDER}

N_ARRAY=(50 75)
N_ARRAY_LENGTH=${#N_ARRAY[*]}

OP_ARRAY=(0.006 0.008 0.01 0.03)
OP_ARRAY_LENGTH=${#OP_ARRAY[*]}

V_ARRAY=(0.01)
V_ARRAY_LENGTH=${#V_ARRAY[*]}

############################################################
#############################################################
############################################################
# Test example

# N_ARRAY=(50) # 75)
# N_ARRAY_LENGTH=${#N_ARRAY[*]}
#
# OP_ARRAY=( 0.01 ) # 0.006 0.01)
# OP_ARRAY_LENGTH=${#OP_ARRAY[*]}
#
# V_ARRAY=(0.01 0.05)
# V_ARRAY_LENGTH=${#V_ARRAY[*]}
#
# dt2=0.01
# C_ITERATIONS=2

############################################################
############################################################
############################################################
# I stands for index
I_MEAN=0
I_SD=1
I_TEMPERATURE=2

# declare Associative arrays (like hashmaps): http://wiki.bash-hackers.org/syntax/arrays
declare -A PRESSURE_ARRAY

# Initialize arrays for storing I_MEAN & I_SD
for (( i = 0; i < ${N_ARRAY_LENGTH}; i++ )); do
  for (( j = 0; j < ${OP_ARRAY_LENGTH}; j++ )); do
    for (( a = 0; a < ${V_ARRAY_LENGTH}; a++ )); do
      PRESSURE_ARRAY["$i,$j,$a,$I_MEAN"]=0 # N_INDEX, OP_INDEX, V_INDEX, I_MEAN
      PRESSURE_ARRAY["$i,$j,$a,$I_SD"]=0 # N_INDEX, OP_INDEX, V_INDEX, I_SD
      PRESSURE_ARRAY["$i,$j,$a,$I_TEMPERATURE"]=0 # N_INDEX, OP_INDEX, V_INDEX, I_TEMPERATURE
    done
  done
done

echo -e "####################################"
for (( a = 0; a < ${V_ARRAY_LENGTH}; a++ )); do
  v=${V_ARRAY[${a}]}

  echo -e "Running analyser with v = $v..."

  BACKUP_DIR_V="${OUTPUT_FOLDER}/it_results/v$v"
  mkdir -p ${BACKUP_DIR_V}

  echo -e "  ************************************"
  for (( i = 0; i < ${N_ARRAY_LENGTH}; i++ )); do
    N=${N_ARRAY[${i}]}

    echo -e "  Running analyser with N = $N..."

    echo -en "    Generating static.dat file...  "
    gen_static
    validate_exit_status

    BACKUP_DIR_N="${BACKUP_DIR_V}/N${N}"
    mkdir -p ${BACKUP_DIR_N}

    for (( j = 0; j < ${OP_ARRAY_LENGTH}; j++ )); do
      OP=${OP_ARRAY[${j}]}

      # unset all inner variables
      unset MAX_ITERATIONS
      unset MIN_ITERATIONS
      unset FPs_MEAN
      unset FPs_SD
      unset IT_TO_EQ_MEAN
      unset IT_TO_EQ_SD
      unset ITERATION
      unset TIME

      echo -e "    ------------------------------------"
      echo -e "    Running analyser with opening (OP) = ${OP}..."

      BACKUP_DIR_OP="${BACKUP_DIR_N}/OP${OP}"
      mkdir -p ${BACKUP_DIR_OP}

      for (( k = 1; k <= ${C_ITERATIONS}; k++ )) ; do
        echo -en "      Generating dynamic.dat file...  "
        gen_dynamic
        validate_exit_status

        echo -en "      Generating output.dat file...  "
        gen_output
        validate_exit_status

        unset FPs
        # Get the array of all FPs from all times of the simulation
        FPs=( $(awk -F "\"*,\"*" '{print $3}' ${SIM_I_T_FP_PRE_TEMP_PATH} ) )
        if [ ${#FPs[@]} -gt ${#FPs_MEAN[@]} ]; then
          MAX_ITERATIONS=${#FPs[@]}
        else
          MAX_ITERATIONS=${#FPs_MEAN[@]}
        fi

        # calculate the media and standard derivation, relative to all previous values, for this step
        # for (( l = 0; l < ${MAX_ITERATIONS}; l++ )); do
        #   FP=$(awk -v a=${FPs["$l"]} 'BEGIN {printf "%.6f\n", a}')
        #
        #   FPs_MEAN["$l"]=$(awk -v a=${FPs_MEAN["$l"]} -v b=${FP} 'BEGIN {printf "%.6f\n", a+b}')
        #   FPs_SD["$l"]=$(awk -v a=${FPs_SD["$l"]} -v b=${FP} 'BEGIN {printf "%.6f\n", a+b^2}')
        # done
        # echo $A

        # Get the value of the number of itereations to reach the equilibrium
        # This is on the 6th row, 2nd column of the time_to_eq.csv file
        IT_TO_EQ=`sed '7q;d' ${SIM_TIME_TO_EQ_PATH} | awk -F "\"*,\"*" '{print $2}'`
        IT_TO_EQ_MEAN=$(awk -v mean=${IT_TO_EQ_MEAN} -v curr=${IT_TO_EQ} \
                      'BEGIN {printf "%.6f\n", mean + curr}')
        IT_TO_EQ_SD=$(awk -v a=${IT_TO_EQ_SD} -v b=${IT_TO_EQ} 'BEGIN {printf "%.6f\n", a+b^2}')

        # if [ ${IT_TO_EQ} -le ${MIN_ITERATIONS:=${IT_TO_EQ}} ]; then
        #   MIN_ITERATIONS=${IT_TO_EQ}
        # fi

        PRESSURE=`sed '8q;d' ${SIM_TIME_TO_EQ_PATH} | awk -F "\"*,\"*" '{print $2}'`
        PRESSURE_ARRAY["$i, $j, $a, $I_MEAN"]=$(awk -v mean=${PRESSURE_ARRAY["$i, $j, $a, $I_MEAN"]} -v curr=${PRESSURE} \
                      'BEGIN {printf "%.6f\n", mean + curr}')
        PRESSURE_ARRAY["$i, $j, $a, $I_SD"]=$(awk -v a=${PRESSURE_ARRAY["$i, $j, $a, $I_SD"]} -v b=${PRESSURE} 'BEGIN {printf "%.6f\n", a+b^2}')

        # Temperature Constant for this system
        PRESSURE_ARRAY["$i, $j, $a, $I_TEMPERATURE"]=`sed '9q;d' ${SIM_TIME_TO_EQ_PATH} | awk -F "\"*,\"*" '{print $2}'`

        # DO NOT BACKUP ANYTHING, EXCEPT NEEDED! output.dat may be too large
        # Backup current iteration
        BACKUP_DIR="${BACKUP_DIR_OP}/I${k}"
        mkdir -p ${BACKUP_DIR}

        # cp ${STATIC_PATH} ${BACKUP_DIR}/
        # cp ${DYNAMIC_PATH} ${BACKUP_DIR}/
        # cp ${SIM_OUTPUT_PATH} ${BACKUP_DIR}/
        cp ${SIM_TIME_TO_EQ_PATH} ${BACKUP_DIR}/
        cp ${SIM_I_T_FP_PRE_TEMP_PATH} ${BACKUP_DIR}/

        PERCENTAGE_COMPLETED=$(bc <<< "scale=6;$k/$C_ITERATIONS * 100")
        echo -e "        ** Completed: $PERCENTAGE_COMPLETED% ** \r" # A % completed value
      done
      echo -e "    [DONE]"

      echo -en "    Generating i_t_fp_N${N}_OP${OP} results file... "

      # for (( l = 0; l < ${#FPs_MEAN[@]}; l++ )); do
      #   ITERATION["$l"]=${l}
      #   TIME["$l"]=$(awk -v a=${l} -v b=${dt2} 'BEGIN {printf "%.6f\n", a*b}')
      #   # FPs_MEAN["$l"]=$(awk -v a=${FPs_MEAN["$l"]:=0} -v b=${C_ITERATIONS} 'BEGIN {printf "%.6f\n", a/b}')
      #   # # Done this way so as to avoid -nan due to negative number caused by decimal precision
      #   # FIRST=$(awk -v a=${FPs_SD["$l"]:=0} -v b=${C_ITERATIONS} 'BEGIN {printf "%.6f\n", a/b}')
      #   # SECOND=$(awk -v c=${FPs_MEAN["$l"]} 'BEGIN {printf "%.6f\n", c^2}')
      #   # FPs_SD["$l"]=$(awk -v a=${FIRST} -v b=${SECOND} 'BEGIN {printf "%.6f\n", sqrt(a-b)}')
      # done

      IT_TO_EQ_MEAN=$(awk -v a=${IT_TO_EQ_MEAN} -v b=${C_ITERATIONS} 'BEGIN {printf "%.6f\n", a/b}')
      FIRST=$(awk -v a=${IT_TO_EQ_SD} -v b=${C_ITERATIONS} 'BEGIN {printf "%.6f\n", a/b}')
      SECOND=$(awk -v c=${IT_TO_EQ_MEAN} 'BEGIN {printf "%.6f\n", c^2}')
      IT_TO_EQ_SD=$(awk -v a=${FIRST} -v b=${SECOND} 'BEGIN {printf "%.6f\n", sqrt(a-b)}')

      PRESSURE_ARRAY["$i, $j, $a, $I_MEAN"]=$(awk -v a=${PRESSURE_ARRAY["$i, $j, $a, $I_MEAN"]} -v b=${C_ITERATIONS} 'BEGIN {printf "%.6f\n", a/b}')
      FIRST=$(awk -v a=${PRESSURE_ARRAY["$i, $j, $a, $I_SD"]} -v b=${C_ITERATIONS} 'BEGIN {printf "%.6f\n", a/b}')
      SECOND=$(awk -v c=${PRESSURE_ARRAY["$i, $j, $a, $I_MEAN"]} 'BEGIN {printf "%.6f\n", c^2}')
      PRESSURE_ARRAY["$i, $j, $a, $I_SD"]=$(awk -v a=${FIRST} -v b=${SECOND} 'BEGIN {printf "%.6f\n", sqrt(a-b)}')

      OUTPUT_TABLE_PATH=`gen_i_t_fp`

      echo -e "E(Time(s)), SD(Time(s))\r" >> ${OUTPUT_TABLE_PATH}
      paste -d ',' <(printf "%s\n" "${IT_TO_EQ_MEAN}") <(printf "%s\n" "${IT_TO_EQ_SD}") \
                  >> ${OUTPUT_TABLE_PATH}
                  #  <(printf "%s\n" "${MIN_ITERATIONS}")

      echo -en "    [DONE]\n"
    done

    echo -e "    ------------------------------------"
    echo -e "  [DONE]"
    echo -e "  ************************************"
  done
  echo -e "[DONE]"
  echo -e "####################################"
done

# # Generate pressure stadistic file
# v="Varying"
# for (( i = 0; i < ${N_ARRAY_LENGTH}; i++ )); do
#   N=${N_ARRAY[$i]}
#   for (( j = 0; j < ${OP_ARRAY_LENGTH}; j++ )); do
#     OP=${OP_ARRAY[$j]}
#     OUTPUT_TABLE_PATH=`gen_pre_t_results` # N & OP fixed; velocity varies
#     for (( a = 0 ; a < ${V_ARRAY_LENGTH} ; a++ )); do
#       v=${V_ARRAY[$a]}
#       paste -d ','  <(printf "%s\n" "${v}") \
#                     <(printf "%s\n" "${PRESSURE_ARRAY["$i, $j, $a, $I_TEMPERATURE"]}") \
#                     <(printf "%s\n" "${PRESSURE_ARRAY["$i, $j, $a, $I_MEAN"]}") \
#                     <(printf "%s\n" "${PRESSURE_ARRAY["$i, $j, $a, $I_SD"]}") \
#                     >> ${OUTPUT_TABLE_PATH}
#     done
#   done
# done

END_TIME=$(date +%s)

EXECUTION_TIME=`expr $END_TIME - $START_TIME`
EXECUTION_TIME_FILE="${OUTPUT_FOLDER}/execution_time.statistics"
rm -f ${EXECUTION_TIME_FILE}
touch ${EXECUTION_TIME_FILE}
echo -e "Execution time: ${EXECUTION_TIME} seconds\r" >> ${EXECUTION_TIME_FILE}
echo -e "Execution time: ${EXECUTION_TIME} seconds"

# Move output folder to parent project's folder
DATE_TIME=$(date +%Y-%m-%d-%H%M%S)
FINAL_OUTPUT_FOLDER=${PROJECT_FOLDER}/output/analyser/${DATE_TIME}
mkdir -p ${FINAL_OUTPUT_FOLDER}
mv ${OUTPUT_FOLDER}/* ${FINAL_OUTPUT_FOLDER}
rm -rf ${OUTPUT_FOLDER}
rm ${FINAL_OUTPUT_FOLDER}/*.dat ${FINAL_OUTPUT_FOLDER}/*.csv # Remove last iteration files

# Move log folder to parent project's folder
mkdir -p ${FINAL_OUTPUT_FOLDER}/logs
mv logs/* ${FINAL_OUTPUT_FOLDER}/logs
rm -r logs
