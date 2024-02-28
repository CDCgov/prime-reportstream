# How to create the `nces_id.csv` table file

A quick descriptions of the steps needed to create the NCES id table. This table changes about once a year. 

## Prepare

Install `CSVKit` https://csvkit.readthedocs.io/en/latest/

## 1. Get the raw data from NCES

Get the raw public school data from the https://nces.ed.gov/ccd download site

1. Select `public`, `non-fisical`, latest year
2. Download the entire Zip file
3. Use the contained csv

Get the raw private school data from https://nces.ed.gov/ccd/elsi/ download site.

1. Select `Private School` as the row
2. Select the latest year available
3. Select School Name (from basic info), Location Zip (from contact info), and School ID as columns
4. Select widest possible area: "All 50 states + DC + Outlying areas"

## 2. Clean the CSV files

The CSV files have comment rows. Run them through `csvclean`. Clean by hand if necessary. 
Remove unwanted columns using `csvcut`. 

## 3. Rename headers for both CSV files

The expected headers are `LZIP`, `SCHNAME`, `NCESID`. 

## 4. Join the files

Use `csvstack` to merge the public and private school files into a single `nces_id.csv` file. 

## 5. Sort the files

Use `csvsort` on the `LZIP` and `SCHNAME` column.

## 6. Name the file

Name the file with the date of the latest data set

## 7. Test

Diff the file with the previous to make sure no errors have been added in. 
Run through a couple of real test k12 files to test that file is working.  