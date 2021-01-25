#!/usr/bin/env bash

# Learn from my mistakes:
#  If you edit the csv's in Excel, be extremely careful when you save.   I've seen at least two problems:
#    1.  The dates get screwed up.  I had to manually set the dates to yyyy-mm-dd format, or it changes them to mm/dd/yyyy
#    2.  It seems to tweak the use of "" for escaping fields, so that while the csv looks the same in Excel, the ascii has changed.

# This runs prime in a variety of ways on csv files and compare outputs to actuals

outputdir=target/csv_test_files
expecteddir=./src/test/csv_test_files/expected
expected_pima=$expecteddir/simplereport-pima.csv
expected_az=$expecteddir/simplereport-az.csv
# Uncomment to force failure
#expected_az=$expecteddir/junk-az.csv
starter_schema=primedatainput/pdi-covid-19

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# let's make it possible for run for different states
RUN_AZ=0
RUN_FL=0
RUN_ND=0
# always should run, but we'll leave this here for now in case that could change at some point
RUN_STANDARD=1
RUN_ALL=0
RUN_MERGE=0

# If no args, run everything.
if [ $# -eq 0 ] 
then
  RUN_ALL=1
fi

for arg in "$@"
do
  case "$arg" in
    fl | FL) RUN_FL=1;;
    az | AZ) RUN_AZ=1;;
    nd | ND) RUN_ND=1;;
    all | ALL) RUN_ALL=1;;
    merge | MERGE) RUN_MERGE=1;;
  esac
done

if [ $RUN_ALL -ne 0 ]
then
  RUN_FL=1
  RUN_AZ=1
  RUN_ND=1
  RUN_STANDARD=1
  RUN_MERGE=1
fi

mkdir -p $outputdir

# Call this after calling .prime ... to generate output data.  Use this to parse the text output to extract the filename
# Sets a global variable $filename to the last string that matches $match_string in the text $prime_output
filename=0
function parse_prime_output_for_filename {
  prime_output=$1
  match_string=$2
  filename="File_not_found"
  ready=0
  for name in $prime_output ; do
    if grep -q "Creating" <<< $name ; then
      ready=1
    fi
    if [ $ready -eq 1 ] ; then 
      if grep -q $match_string <<< $name ; then
        filename=$name
      fi
    fi
  done

  if [ -f $filename ] ; then
    printf "Data generation ${GREEN}TEST PASSED${NC}: successfully generated data file $filename\n"
  else 
    printf "${RED}*** ERROR ***: ./prime did not generate a file that matches $match_string${NC}\n"
    printf "Output of Prime run is: $prime_output\n"
    exit
  fi
}

function compare_files {
  schemaName=$1
  expected=$2
  actual=$3
  diff $expected $actual >/dev/null
  retval=$?
  if [ $retval -gt 0 ] ; then 
    printf "${RED}*** ERROR ***: DIFFERENCES FOUND for $schemaName.  Run this to see the diff:${NC}\n"
    printf "diff $expected $actual\n"
  else
    printf "File comparison ${GREEN}TEST PASSED${NC}: No differences found in $schemaName data. This is the $schemaName output file:\n\t$actual\n"
  fi
}

# Used in merge testing, below.
function count_lines {
  filename=$1
  searchStr=$2
  expectedNum=$3
  numlines=$(grep -c $searchStr $filename)
  if [ $numlines -ne $expectedNum ] ; then
    printf "${RED}*** ERROR ***:Expecting $expectedNum lines of $searchStr data, but got $numlines${NC}\n"
    printf "Problem found in $filename\n"
  fi
}

# run the standard
if [ $RUN_STANDARD -ne 0 ]
then
  echo First run our canned test file simplereport.csv thru the gauntlet
  text=$(./prime data --input-schema $starter_schema --input ./src/test/csv_test_files/input/simplereport.csv --output-dir $outputdir --route)
fi

AZ_FILE_SEARCH_STR="/az.*\.csv"
PIMA_FILE_SEARCH_STR="/pima.*\.csv"

# run arizona and pima
if [ $RUN_AZ -ne 0 ]
then
  parse_prime_output_for_filename "$text" $AZ_FILE_SEARCH_STR
  actual_az=$filename
  compare_files "SimpleReport->AZ"   $expected_az   $actual_az

  parse_prime_output_for_filename "$text" $PIMA_FILE_SEARCH_STR
  actual_pima=$filename
  compare_files "SimpleReport->PIMA" $expected_pima $actual_pima

  # Add tx.

  # Now read the data back in to their own schema and export again.
  # AZ again
  echo Test sending AZ data into its own Schema:
  text=$(./prime data --input-schema az/az-covid-19 --input $actual_az --output-dir $outputdir)
  parse_prime_output_for_filename "$text" $AZ_FILE_SEARCH_STR
  actual_az2=$filename
  compare_files "AZ->AZ" $expected_az $actual_az2

  # Pima again
  echo Test sending Pima data into its own Schema:
  text=$(./prime data --input-schema az/pima-az-covid-19 --input $actual_pima --output-dir $outputdir)
  parse_prime_output_for_filename "$text" $PIMA_FILE_SEARCH_STR
  actual_pima2=$filename
  compare_files "PIMA->PIMA" $expected_pima $actual_pima2

  echo And now generate some fake simplereport data
  text=$(./prime data --input-fake 50 --input-schema $starter_schema --output-dir $outputdir)
  parse_prime_output_for_filename "$text" "/pdi-covid-19"
  fake_data=$filename

  echo Now send that fake data thru the router.
  # Note that there's no actuals to compare to here.
  text=$(./prime data --input-schema $starter_schema --input $fake_data --output-dir $outputdir --route)
  # Find the AZ file
  parse_prime_output_for_filename "$text" $AZ_FILE_SEARCH_STR
  actual_az3=$filename
  # Find the Pima file
  parse_prime_output_for_filename "$text" $PIMA_FILE_SEARCH_STR
  actual_pima3=$filename

  echo Now send _those_ results back in to their own schema and export again!
  # AZ again again.  This time we can compare the two actuals.
  echo Test sending AZ data generated from fake data into its own Schema:
  text=$(./prime data --input-schema az/az-covid-19 --input $actual_az3 --output-dir $outputdir)
  parse_prime_output_for_filename "$text" $AZ_FILE_SEARCH_STR
  actual_az4=$filename
  compare_files "AZ->AZ" $actual_az3 $actual_az4

  # Pima again again.  Compare the two actuals
  echo Test sending Pima data generated from fake data into its own Schema:
  text=$(./prime data --input-schema az/pima-az-covid-19 --input $actual_pima3 --output-dir $outputdir)
  parse_prime_output_for_filename "$text" $PIMA_FILE_SEARCH_STR
  actual_pima4=$filename
  compare_files "PIMA->PIMA" $actual_pima3 $actual_pima4
fi

# run florida
if [ $RUN_FL -ne 0 ]
then
  FL_FILE_SEARCH_STR="/fl.*\.csv"
  # FLORIDA, MAN
  echo Generate fake FL data
  text=$(./prime data --input-fake 50 --input-schema fl/fl-covid-19 --output-dir $outputdir --target-state FL)
  parse_prime_output_for_filename "$text" $FL_FILE_SEARCH_STR
  fake_fl=$filename

  echo Now send that fake FL data through the router.
  text=$(./prime data --input-schema fl/fl-covid-19 --input $fake_fl --output-dir $outputdir)
  parse_prime_output_for_filename "$text" $FL_FILE_SEARCH_STR
  fake_fl2=$filename
  compare_files "Fake FL Orig -> Fake FL2" $fake_fl $fake_fl2

  echo Now send _those_ FL results back in to their own schema and export again!
  text=$(./prime data --input-schema fl/fl-covid-19 --input $fake_fl2 --output-dir $outputdir)
  fake_fl3=$filename
  compare_files "FakeFL2 -> FakeFL3" $fake_fl2 $fake_fl3
fi

# run north dakota
if [ $RUN_ND -ne 0 ]
then
  echo Generate fake ND data, HL7!
  text=$(./prime --input_fake 50 --input_schema nd/nd-covid-19 --output_dir $outputdir --target-state ND --output_hl7)
  parse_prime_output_for_filename "$text" "/nd"

  echo Now send that fake ND data through the router
  # TODO: can we import HL7?
  # TODO: how do we compare HL7?

  echo Now those _those_ ND results back in to their own schema and export again!
  # TODO: once we've imported HL7 we can finish this step
fi

if [ $RUN_MERGE -ne 0 ]
then
  STRAC_FILE_SEARCH_STR="/strac-covid-19.*\.csv"
  numitems=5
  echo Merge testing.  First, generate some fake STRAC data
   # Hack: put some unique strings in each one, so we can count lines.
  text=$(./prime data --input-fake $numitems --input-schema strac/strac-covid-19 --output-dir $outputdir --target-county lilliput)
  parse_prime_output_for_filename "$text" $STRAC_FILE_SEARCH_STR
  fake1=$filename

 echo More fake STRAC data
  text=$(./prime data --input-fake $numitems --input-schema strac/strac-covid-19 --output-dir $outputdir --target-county brobdingnag)
  parse_prime_output_for_filename "$text" $STRAC_FILE_SEARCH_STR
  fake2=$filename

 echo 3rd file of fake STRAC data
  text=$(./prime data --input-fake $numitems --input-schema strac/strac-covid-19 --output-dir $outputdir --target-county houyhnhnm)
  parse_prime_output_for_filename "$text" $STRAC_FILE_SEARCH_STR
  fake3=$filename

  echo Now testing merge:
  text=$(./prime data --merge $fake1,$fake2,$fake3 --input-schema strac/strac-covid-19 --output-dir $outputdir )
  parse_prime_output_for_filename "$text" $STRAC_FILE_SEARCH_STR
  merged_file=$filename

  count_lines $merged_file brobdingnag $numitems
  count_lines $merged_file lilliput $numitems
  count_lines $merged_file houyhnhnm $numitems
  let total=($numitems \* 3)+1 
  # All the lines should have a comma
  count_lines $merged_file , $total
fi

exit 0

