# How to create the `nces_id.csv` table file

A quick descriptions of the steps needed to create the NCES id table. 

## Prepare

Install `CSVKit` [https://csvkit.readthedocs.io/en/latest/ ]()

## 1. Get the raw data from NCES

Get the raw data from [https://nces.ed.gov/ccd/elsi/]() download site.
Use the table generator tool. We need to run the tool twice; first for public school data and
second for private school data. Specific steps:

1. Select `Public School` or `Private School` as the row
2. Select the latest year available
3. Select School Name (from basic info), Location Zip (from contact info), and School ID as columns
4. Select widest possible area: "All 50 states + DC + Outlying areas"

Repeat for private schools. You should have two csv files now that will be needed to clean and merged.  

## 2. Clean the CSV files

The CSV files have comment rows. Run them through `csvclean`. Clean by hand if necessary. 
Remove unwanted columns using `csvcut`. 

## 3. Rename headers for both CSV files

The expected headers are `SCHNAME`, `LZIP`, `NCESID`. 

## 4. Join the files

Use `csvstack` to merge the public and private school files into a single `nces_id.csv` file. 

## 5. Sort the files

Use `csvsort` on the `LZIP` column. 