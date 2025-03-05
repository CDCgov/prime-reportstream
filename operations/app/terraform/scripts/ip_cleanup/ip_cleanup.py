"""
ip_cleanup.py

This script processes a list of whitelisted IP addresses, looks up details using IPWhois,
and categorizes them as Government, ISP, or Commercial entities.

Requirements:
- Python 3.x
- Install dependencies using: pip install -r requirements.txt
"""

import csv
from ipwhois import IPWhois  # Importing IPWhois library to fetch IP ownership details

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
        ipwhois = IPWhois(ip_address)
        result = ipwhois.lookup_rdap()  # Perform RDAP lookup to fetch details

        # Extract relevant details from the response
        network = result.get('network', {})  # Network details (may be None)
        entities = result.get('entities', [])  # Associated entities
        country = result.get('asn_country_code', 'Unknown')  # Country code
        cidr = network.get('cidr', 'N/A')  # Network range in CIDR notation
        name = network.get('name', 'N/A')  # Network name (if available)
        org = result.get('asn_description', 'N/A')  # Organization description

        return {
            'IP Address': ip_address,
            'Organization': org,
            'Network Name': name,
            'CIDR': cidr,
            'Country': country,
            'Entities': ", ".join(entities) if entities else 'N/A'
        }
    except Exception as e:
        # Handle errors gracefully, returning the error message
        return {'IP Address': ip_address, 'Error': str(e)}

def process_ips_and_export(filename, output_csv):
    """
    Reads a list of IP addresses from a file, retrieves details for each, and saves them to a CSV file.

    Args:
        filename (str): Path to the input file containing IP addresses.
        output_csv (str): Path to the output CSV file where results will be saved.
    """
    ips = read_ips_from_file(filename)  # Read IP addresses from input file
    results = [get_ip_details(ip) for ip in ips]  # Lookup details for each IP

    # Write results to a CSV file
    with open(output_csv, 'w', newline='') as csvfile:
        fieldnames = ['IP Address', 'Organization', 'Network Name', 'CIDR', 'Country', 'Entities', 'Error']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)

        writer.writeheader()  # Write column headers
        for row in results:
            writer.writerow(row)  # Write each IP lookup result to the file

    print(f"Results saved to {output_csv}")

# Example usage:
if __name__ == "__main__":
    input_filename = 'ips.txt'  # Replace with the actual input file containing IPs
    output_filename = 'ip_results.csv'  # Output file name
    process_ips_and_export(input_filename, output_filename)  # Process IPs and save results
