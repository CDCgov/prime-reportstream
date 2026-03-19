import requests
import csv
import sys
import os
import pandas as pd
import re
from io import StringIO

# Output file path for enriched ZIP code data
OUTPUT_CSV = "../../prime-router/metadata/tables/local/zip-code-data.csv"

# URL to fetch national county FIPS lookup data from the U.S. Census Bureau
COUNTY_URL = "https://www2.census.gov/geo/docs/reference/codes/files/national_county.txt"

def fetch_census_county_lookup():
    """
    Downloads the national county lookup file from the Census Bureau
    and returns it as a pandas DataFrame with cleaned column names.
    """
    print("Downloading Census county lookup..")
    response = requests.get(COUNTY_URL, verify=False)  # Disabling SSL verification
    df = pd.read_csv(StringIO(response.text), delimiter=",", dtype=str)
    df.columns = ["state_abbr", "state_fips", "county_fips", "county", "FIPS Class Code"]
    print(df)
    return df

def get_zip_info(zip_code, headers):
    """
    Queries the HUD USPS API for information about a ZIP code.
    Returns a DataFrame with city, state, and FIPS codes.
    """
    url = f"https://www.huduser.gov/hudapi/public/usps?type=2&query={zip_code}"
    rows = []
    try:
        response = requests.get(url, headers=headers)
        response.raise_for_status()
        results = response.json().get("data", {}).get("results", [])
        if not results:
            print(f"No data for ZIP {zip_code}")
            return None

        for item in results:
            city = item.get("city", "").title()
            state = item.get("state")
            county_fips = item.get("geoid")[2:5]   # Characters 3–5 are the county FIPS
            state_fips = item.get("geoid")[:2]     # Characters 1–2 are the state FIPS
            rows.append({
                "zip_code": zip_code,
                "city": city,
                "county_fips": county_fips,
                "state_fips": state_fips,
                "state": state
            })

        return pd.DataFrame(rows)

    except Exception as e:
        print(f"Error processing ZIP {zip_code}: {e}")
        return pd.DataFrame(rows)

def main():
    # Download county lookup data from the Census
    gaz = fetch_census_county_lookup()

    # Validate command-line arguments
    if len(sys.argv) < 3:
        print("Usage: python lookup_zips_from_file.py <input_file> <USPS_API_token>")
        sys.exit(1)

    input_file = sys.argv[1]
    token = sys.argv[2]

    # Ensure the input file exists
    if not os.path.exists(input_file):
        print(f"File not found: {input_file}")
        sys.exit(1)

    # Set up authorization header for HUD API
    headers = {
        "accept": "application/json",
        "Authorization": f"Bearer {token}"
    }

    # Open the input file (list of ZIP codes) and output CSV for writing
    with open(input_file, "r") as infile, open(OUTPUT_CSV, "a", newline="") as outfile:
        writer = csv.writer(outfile, delimiter=',')
        writer.writerow(['state_fips', 'state', 'state_abbr', 'zip_code', 'county', "city"])

        # Process each ZIP code in the input file
        for line in infile:
            zip_code = re.sub(r'\D', '', line)  # Remove all non-digit characters
            print("zip_code " + zip_code)

            if not zip_code.isdigit():
                print("NOT zip_code")
                continue

            print(f"Looking up ZIP: {zip_code}")
            info = get_zip_info(zip_code, headers)

            if not info.empty:
                # Merge ZIP info with Census county data using FIPS codes
                merged = pd.merge(gaz, info, on=["state_fips", "county_fips"], how="inner")

                # Select and deduplicate the relevant columns
                new_rows = merged[['state_fips', 'state', 'state_abbr', 'zip_code', 'county', "city"]].drop_duplicates()

                print(merged.values)
                writer.writerows(new_rows.values.tolist())
            else:
                print(f"Skipped ZIP {zip_code}")

# Entry point
if __name__ == "__main__":
    main()
