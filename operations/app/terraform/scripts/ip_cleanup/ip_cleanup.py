import csv
from ipwhois import IPWhois

def read_ips_from_file(filename):
    with open(filename, 'r') as file:
        return [line.strip() for line in file.readlines()]

def get_ip_details(ip_address):
    try:
        ipwhois = IPWhois(ip_address)
        result = ipwhois.lookup_rdap()
        network = result.get('network', {})
        entities = result.get('entities', [])
        city = result.get('asn_country_code', 'Unknown')
        cidr = network.get('cidr', 'N/A')
        name = network.get('name', 'N/A')
        org = result.get('asn_description', 'N/A')
        return {
            'IP Address': ip_address,
            'Organization': org,
            'Network Name': name,
            'CIDR': cidr,
            'Country': city,
            'Entities': ", ".join(entities) if entities else 'N/A'
        }
    except Exception as e:
        return {'IP Address': ip_address, 'Error': str(e)}

def process_ips_and_export(filename, output_csv):
    ips = read_ips_from_file(filename)
    results = [get_ip_details(ip) for ip in ips]
    
    with open(output_csv, 'w', newline='') as csvfile:
        fieldnames = ['IP Address', 'Organization', 'Network Name', 'CIDR', 'Country', 'Entities', 'Error']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        for row in results:
            writer.writerow(row)
    
    print(f"Results saved to {output_csv}")

# Example usage
input_filename = 'ips.txt'  # Replace with your input file path
output_filename = 'ip_results.csv'  # Replace with your desired output file path
process_ips_and_export(input_filename, output_filename)
