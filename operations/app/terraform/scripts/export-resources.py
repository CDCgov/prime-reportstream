#!/usr/bin/env python

import subprocess
import csv
import argparse

# Set up argument parser
parser = argparse.ArgumentParser( description="Fetch Azure resources and export them to a CSV file." ) ;
parser.add_argument(
    "--output-file",
    type=str,
    default="azure-resources--python.csv",
    help="Path to the output CSV file (default: azure-resources--python.csv)"
) ;

# Parse arguments
args = parser.parse_args() ;
output_file = args.output_file ;

# Command to fetch Azure resources
cmd = [
    "az", "resource", "list",
    "--query", "[].{\"Location\":location, \"Name\":name, \"Resource Group\":resourceGroup}",
    "--output", "tsv"
] ;

# Run the Azure CLI command
result = subprocess.run( cmd, stdout=subprocess.PIPE, text=True ) ;

# Write header and data to CSV file
with open( output_file, "w", newline="" ) as csvfile:
    writer = csv.writer( csvfile ) ;
    writer.writerow( ["Location", "Name", "Resource Group"] ) ;
    for line in result.stdout.splitlines():
        writer.writerow( line.split( "\t" ) ) ;

# Print the contents of the generated CSV
with open( output_file, "r" ) as csvfile:
    print( csvfile.read() ) ;
