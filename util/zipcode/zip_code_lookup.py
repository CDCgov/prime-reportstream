import requests
import csv
import sys
import os
import pandas as pd
import re
from io import StringIO

OUTPUT_CSV = "../../prime-router/metadata/tables/local/zip-code-data.csv"
COUNTY_URL = "https://www2.census.gov/geo/docs/reference/codes/files/national_county.txt"


def fetch_census_county_lookup():
    print("Downloading Census county lookup..")
    response = requests.get(COUNTY_URL, verify=False)
    df = pd.read_csv(StringIO(response.text), delimiter=",", dtype=str)
    df.columns = ["state_abbr", "state_fips", "county_fips", "county", "FIPS Class Code"]
    print(df)
    return df

def get_zip_info(zip_code,headers):
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
            county_fips = item.get("geoid")[2:5]
            state_fips = item.get("geoid")[:2]
            rows.append({"zip_code":zip_code, "city":city, "county_fips":county_fips, "state_fips":state_fips,"state":state})
        return pd.DataFrame(rows)
    except Exception as e:
        print(f"Error processing ZIP {zip_code}: {e}")
        return pd.DataFrame(rows)


def main():
    gaz = fetch_census_county_lookup()

    if len(sys.argv) < 3:
        print("Usage: python lookup_zips_from_file.py <input_file> <USPS_API_token>")
        sys.exit(1)

    input_file = sys.argv[1]
    token = sys.argv[2]

    if not os.path.exists(input_file):
        print(f"File not found: {input_file}")
        sys.exit(1)

    headers = {
        "accept": "application/json",
        "Authorization": f"Bearer {token}"
    }
    with open(input_file, "r") as infile, open(OUTPUT_CSV, "a", newline="") as outfile:
        writer = csv.writer(outfile,delimiter=',')
        writer.writerow(['state_fips', 'state', 'state_abbr','zip_code','county',"city"])
        for line in infile:
            zip_code = re.sub(r'\D', '', line)
            print("zip_code " + zip_code)
            if not zip_code.isdigit():
                print("NOT zip_code")
                continue
            print(f"Looking up ZIP: {zip_code}")
            info = get_zip_info(zip_code,headers)
            if not info.empty:
                merged = pd.merge(gaz, info, on=["state_fips","county_fips"], how="inner")
                new_rows = merged[['state_fips', 'state', 'state_abbr','zip_code','county',"city"]].drop_duplicates()
                print(merged.values)
                writer.writerows(new_rows.values.tolist())

            else:
                print(f"Skipped ZIP {zip_code}")



if __name__ == "__main__":
    main()