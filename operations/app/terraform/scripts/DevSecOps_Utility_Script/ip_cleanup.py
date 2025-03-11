"""ip_cleanup.py
This script processes a list of whitelisted IP addresses, looks up details using IPWhois, and categorizes them.
The script now supports command-line arguments to specify input and output files without code changes.
It handles CSV files containing IP addresses that may be quoted or unquoted.

Requirements:
- Python 3.x
- Install dependencies using: pip install -r requirements.txt

Usage:
- Basic: python ip_cleanup.py
- Custom files: python ip_cleanup.py -i custom_ips.csv -o results.csv
- Help: python ip_cleanup.py --help
"""

import csv
import argparse
import os
import sys
from ipwhois import IPWhois  # Importing IPWhois library to fetch IP ownership details

# Default configuration parameters
DEFAULT_INPUT_FILENAME = 'ips.csv'  # Default input file containing IPs (changed to CSV)
DEFAULT_OUTPUT_FILENAME = 'ip_results.csv'  # Default output file name
CSV_FIELD_NAMES = ['IP Address', 'Organization', 'Network Name', 'CIDR', 'Country', 'Entities', 'Error']


def parse_arguments():
    """
    Parse command line arguments with sensible defaults.
   
    Returns:
        argparse.Namespace: The parsed command line arguments.
    """
    # Set up argument parser with description
    parser = argparse.ArgumentParser(description='Process IP addresses and lookup their details.')
   
    # Add input file argument with default value
    parser.add_argument('-i', '--input', default=DEFAULT_INPUT_FILENAME,
                        help=f'Path to the input CSV file containing IP addresses (default: {DEFAULT_INPUT_FILENAME})')
   
    # Add output file argument with default value
    parser.add_argument('-o', '--output', default=DEFAULT_OUTPUT_FILENAME,
                        help=f'Path to the output CSV file (default: {DEFAULT_OUTPUT_FILENAME})')
   
    return parser.parse_args()


def read_ips_from_csv(filename):
    """
    Read IP addresses from a CSV file, handling one IP address per column.
    Process the file line by line to handle large files efficiently.
   
    Args:
        filename (str): Path to the CSV file
       
    Returns:
        generator: A generator yielding IP addresses
    """
    unique_ips = set()
   
    with open(filename, 'r') as csvfile:
        # Process file line by line
        for line in csvfile:
            # Skip empty lines
            if not line.strip():
                continue
               
            # Split the line by commas to get values
            values = line.strip().split(',')
           
            for value in values:
                # Clean the value (remove quotes and spaces)
                ip = value.strip().strip('"\'')
               
                # Skip empty values
                if not ip:
                    continue
               
                # Only yield if it's a unique IP and looks like an IP (contains dots)
                if ip not in unique_ips and '.' in ip:
                    unique_ips.add(ip)
                    yield ip


def get_ip_details(ip_address):
    """
    Fetches ownership and network details for a given IP address using IPWhois.

    Args:
        ip_address (str): The IP address to lookup.

    Returns:
        dict: A dictionary containing IP address details including organization, network name,
              CIDR, country, and associated entities.
    """
    try:
        # Initialize IPWhois with the IP address
        ipwhois = IPWhois(ip_address)
       
        # Perform RDAP lookup to fetch details
        result = ipwhois.lookup_rdap()
       
        # Extract relevant details from the response
        network = result.get('network', {})  # Network details (may be None)
        entities = result.get('entities', [])  # Associated entities
        country = result.get('asn_country_code', 'Unknown')  # Country code
        cidr = network.get('cidr', 'N/A')  # Network range in CIDR notation
        name = network.get('name', 'N/A')  # Network name (if available)
        org = result.get('asn_description', 'N/A')  # Organization description

        # Return dictionary with all the details
        return {
            'IP Address': ip_address,
            'Organization': org,
            'Network Name': name,
            'CIDR': cidr,
            'Country': country,
            'Entities': ", ".join(entities) if entities else 'N/A',
            'Error': ''
        }
    except Exception as e:
        # Handle errors gracefully, returning the error message with the IP
        return {
            'IP Address': ip_address,
            'Organization': 'N/A',
            'Network Name': 'N/A',
            'CIDR': 'N/A',
            'Country': 'N/A',
            'Entities': 'N/A',
            'Error': str(e)
        }


def process_ips_and_export(input_file, output_file):
    """
    Reads IP addresses from a CSV file, retrieves details for each, and saves them to a CSV file.
    Optimized for large files by processing one IP at a time.

    Args:
        input_file (str): Path to the input CSV file containing IP addresses.
        output_file (str): Path to the output CSV file where results will be saved.
    """
    # Check if input file exists
    if not os.path.exists(input_file):
        print(f"Error: Input file '{input_file}' not found.")
        sys.exit(1)
   
    # Inform user about the input file being processed
    file_size_kb = os.path.getsize(input_file) / 1024
    print(f"Processing IP addresses from {input_file} ({file_size_kb:.2f} KB)...")
   
    # Open output file for immediate writing of results
    with open(output_file, 'w', newline='') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=CSV_FIELD_NAMES)
        writer.writeheader()  # Write column headers
       
        processed_count = 0
       
        # Generate IPs from CSV file one at a time
        ip_generator = read_ips_from_csv(input_file)
       
        for ip in ip_generator:
            # Show progress
            processed_count += 1
            print(f"Processing IP #{processed_count}: {ip}")
           
            # Get details and write to CSV
            result = get_ip_details(ip)
            writer.writerow(result)
           
            # Flush periodically to ensure data is written
            if processed_count % 10 == 0:
                csvfile.flush()
   
    print(f"Processing complete. {processed_count} IP addresses processed.")
    print(f"Results successfully saved to {output_file}")


# Main execution
if __name__ == "__main__":
    # Parse command-line arguments
    args = parse_arguments()
   
    # Process IPs and save results using the provided or default paths
    process_ips_and_export(args.input, args.output)
