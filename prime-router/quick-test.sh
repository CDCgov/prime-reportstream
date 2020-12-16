#!/usr/bin/env bash

# Run prime on csv file and compare outputs to actuals

outputdir=target/csv_test_files
expecteddir=./src/test/csv_test_files/expected
expected_pima=$expecteddir/simplereport-pima.csv
expected_az=$expecteddir/simplereport-az.csv
#expected_az=$expecteddir/junk-az.csv

mkdir -p $outputdir

text=$(./prime --input_schema primedatainput/pdi-covid-19 --input ./src/test/csv_test_files/input/simplereport.csv --output_dir $outputdir --route)

# todo Make this iterate on a list (pima, az, tx, etc).
for name in $text ; do
  if grep -q "/az" <<< $name ; then   #   if [[ "$name" =~ "/az" ]] ; then  
    diff $expected_az $name >/dev/null
    retval=$?
    if [ $retval -gt 0 ] ; then 
      printf "ERROR: DIFFERENCES FOUND for AZ.  Run this to see the diff:\n"
      printf "diff $expected_az $name\n"
    else
      printf "Test passed: No differences found in AZ data. This is the AZ output file:\n\t$name\n"
    fi
    echo
  fi

  if grep -q "/pima" <<< $name ; then   #   if [[ "$name" =~ "/pima" ]] ; then  
    diff $expected_pima $name >/dev/null
    retval=$?
    if [ $retval -gt 0 ] ; then 
      printf "ERROR: DIFFERENCES FOUND for PIMA.  Run this to see the diff:\n"
      printf "diff $expected_pima $name\n"
    else
      printf "Test passed: No differences found in PIMA data. This is the PIMA output file:\n\t$name\n"
    fi
    echo
  fi
done











