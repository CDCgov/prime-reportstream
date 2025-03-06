"""ip_cleanup.py
This script processes a list of whitelisted IP addresses, looks up details using IPWhois, and categorizes them.

Requirements:
- Python 3.x
- Install dependencies using: pip install -r requirements.txt

Usage:
- Basic: python ip_cleanup.py
- Custom files: python ip_cleanup.py -i custom_ips.txt -o results.csv
- Help: python ip_cleanup.py --help
"""

import csv
import argparse
from ipwhois import IPWhois  # Importing IPWhois library to fetch IP ownership details

# Default configuration parameters
DEFAULT_INPUT_FILENAME = 'ips.txt'  # Default input file containing IPs
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
                        help=f'Path to the input file containing IP addresses (default: {DEFAULT_INPUT_FILENAME})')
   
    # Add output file argument with default value
    parser.add_argument('-o', '--output', default=DEFAULT_OUTPUT_FILENAME,
                        help=f'Path to the output CSV file (default: {DEFAULT_OUTPUT_FILENAME})')
   
    return parser.parse_args()


def read_ips_from_file(filename):
    """
    Reads a file containing a list of IP addresses.
   
    Args:
        filename (str): The name of the input file containing IP addresses.
   
    Returns:
        list: A list of IP addresses as strings.
    """
    with open(filename, 'r') as file:
        return [line.strip() for line in file.readlines()]


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
            'Entities': ", ".join(entities) if entities else 'N/A'
        }
    except Exception as e:
        # Handle errors gracefully, returning the error message with the IP
        return {'IP Address': ip_address, 'Error': str(e)}


def process_ips_and_export(input_file, output_file):
    """
    Reads a list of IP addresses from a file, retrieves details for each, and saves them to a CSV file.

    Args:
        input_file (str): Path to the input file containing IP addresses.
        output_file (str): Path to the output CSV file where results will be saved.
    """
    # Inform user about the input file being processed
    print(f"Reading IP addresses from {input_file}...")
   
    # Read IP addresses from input file
    ips = read_ips_from_file(input_file)
    print(f"Found {len(ips)} IP addresses to process.")
   
    # Process each IP address with progress indication
    print("Looking up IP details...")
    results = []
    for idx, ip in enumerate(ips, 1):
        print(f"Processing IP {idx}/{len(ips)}: {ip}")
        results.append(get_ip_details(ip))
   
    # Write results to the CSV file
    print(f"Writing results to {output_file}...")
    with open(output_file, 'w', newline='') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=CSV_FIELD_NAMES)
       
        writer.writeheader()  # Write column headers
        for row in results:
            writer.writerow(row)  # Write each IP lookup result to the file
   
    print(f"Results successfully saved to {output_file}")


# Main execution
if __name__ == "__main__":
    # Parse command-line arguments
    args = parse_arguments()
   
    # Process IPs and save results using the provided or default paths
    process_ips_and_export(args.input, args.output)
