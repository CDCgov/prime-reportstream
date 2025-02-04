#!/usr/bin/env python3

"""
Fetch Azure resources and export them to a CSV file.

This script retrieves Azure resources using the Azure CLI and saves them to a CSV file.
Users can customize the fields they want to include using the --output-headers parameter.

Requirements:
  - Python 3.x
  - Azure CLI (`az` command)
  - Logged-in Azure account (`az login`)

Usage:
  python export_azure_resources.py --output-file my-resources.csv
  python export_azure_resources.py --output-headers 'Location:location,Name:name,Resource Group:resourceGroup'
  python export_azure_resources.py --suppress-output
  python export_azure_resources.py --help

Author: Your Name
"""

import subprocess
import csv
import argparse
import shutil
import sys
import re

# Default headers (matches PowerShell script)
DEFAULT_OUTPUT_HEADERS = '"Location":location,"Name":name,"Resource Group:resourceGroup"'

def check_azure_cli():
    """Check if Azure CLI is installed and accessible."""
    if not shutil.which( "az" ):
        print( "\nError: Azure CLI is not installed or not found in PATH." )
        print( "➡ Please install Azure CLI from https://aka.ms/installazurecli\n" )
        sys.exit( 1 )

def check_azure_login():
    """Check if the user is logged into Azure."""
    try:
        subprocess.run(
            ["az", "account", "show"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=True
        )
    except subprocess.CalledProcessError:
        print( "\nError: You are not logged in to Azure." )
        print( "➡ Run 'az login' and try again.\n" )
        sys.exit( 1 )

def parse_headers( header_string ):
    """Convert PowerShell-like headers to CSV format and Azure CLI query format."""
    ## 1. Remove all double quotes from the input
    header_string = header_string.replace( '"', '' )
    ## 2. Split headers into (column name, Azure field ID)
    header_pairs = [header.strip().split( ":" ) for header in header_string.split( "," )]
    if not all( len( pair ) == 2 for pair in header_pairs ):
        print( "\nError: Invalid --output-headers format." )
        print( "➡ Expected format: Column1:azureField1,Column2:azureField2" )
        sys.exit( 1 )
    ## 3. Ensure CSV headers are quoted only if they contain spaces
    csv_headers = [f'"{pair[0]}"' if " " in pair[0] else pair[0] for pair in header_pairs]
    # print( csv_headers )
    ## 4. Construct the valid Azure CLI JMESPath query format
    azure_query = ", ".join( [f'"{pair[0]}":{pair[1]}' for pair in header_pairs] )
    # print( azure_query )
    return csv_headers, azure_query

def fetch_azure_resources( query ):
    """Run Azure CLI command to fetch resources based on dynamic query."""
    cmd = [
        "az", "resource", "list",
        "--query", f"[].{{{query}}}",
        "--output", "tsv"
    ]
    try:
        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=True
        )
        ## Sort the full record instead of isolating fields
        return sorted( result.stdout.splitlines(), key=str )

    except subprocess.CalledProcessError as e:
        print( "\nError: Failed to fetch Azure resources." )
        print( f"➡ Azure CLI error: {e.stderr.strip()}\n" )
        sys.exit( 1 )

def write_csv( output_file, csv_headers, data ):
    """Write Azure resource data to a CSV file, ensuring all fields are quoted."""
    ## Ensure all headers are explicitly quoted
    cleaned_headers = [header.strip('"') for header in csv_headers]
    quoted_headers = [f'{header}' for header in cleaned_headers]

    with open( output_file, "w", newline="\n", encoding="utf-8" ) as csvfile:
        ## Force quotes for all fields
        writer = csv.writer( csvfile, quoting=csv.QUOTE_ALL )
        ## Write quoted headers
        writer.writerow( quoted_headers )
        for line in data:
            ## Trim spaces and ensure consistent line endings
            values = [col.strip() for col in line.split( "\t" )]
            if len( values ) == len( quoted_headers ):
                writer.writerow( values )
            else:
                print(f"Skipping malformed line: {line}")

def main():
    """Main script execution."""
    parser = argparse.ArgumentParser( description="Fetch Azure resources and export them to a CSV file." )
    parser.add_argument(
        "--output-file",
        type=str,
        default="azure-resources.python.csv",
        help="Path to the output CSV file (default: azure-resources.csv)"
    )
    parser.add_argument(
        "--output-headers",
        type=str,
        default=DEFAULT_OUTPUT_HEADERS,
        help="Comma-separated list of headers in 'ColumnName:AzureField' format."
    )
    parser.add_argument(
        "--suppress-output",
        action="store_true",
        help="Suppress printing the CSV content to the console"
    )

    args = parser.parse_args()
    output_file = args.output_file
    output_headers = args.output_headers
    suppress_output = args.suppress_output

    print( "\nChecking Azure CLI availability..." )
    check_azure_cli()

    print( "Checking Azure authentication..." )
    check_azure_login()

    print( "\nParsing output headers..." )
    csv_headers, azure_query = parse_headers( output_headers )

    print( "Fetching Azure resources..." )
    data = fetch_azure_resources( azure_query )

    print( f"\nWriting data to '{output_file}'..." )
    write_csv( output_file, csv_headers, data )

    print( "\nAzure resources successfully exported!" )
    print( f"Saved as: {output_file}\n" )

    if not suppress_output:
        print("CSV Content (First 10 Lines):\n" )
        with open( output_file, "r", encoding="utf-8" ) as csvfile:
            for i, line in enumerate( csvfile ):
                if i >= 10: break
                print( line.strip() )

if __name__ == "__main__":
    main()
