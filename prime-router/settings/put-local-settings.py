#!/usr/bin/env python3

# Put the settings in the organization.json and senders.json, and the receivers.json files
# into the local settings service
import json
import requests
import yaml
import time
import argparse
import os
import datetime
import sys
from email.utils import parsedate_to_datetime

parser = argparse.ArgumentParser(description='Put org file into a prime service')
parser.add_argument('--conn-retries', type=int, default=10)
parser.add_argument('--check-last-modified', action='store_true', default=False)
parser.add_argument('host', default='localhost')
parser.add_argument('org_file', default='organizations.yml')
args = parser.parse_args()
host = args.host
protocol = "http"
org_file = args.org_file

base_url = f"{protocol}://{host}:7071/api"
headers = {"Authorization": "bearer xyz", "Content-Type": "application/json"}


def parse_file(file_name):
    with open(file_name) as file:
        data = yaml.full_load(file)
        return data


def put_org(org):
    justOrg = org.copy()
    if "senders" in justOrg:
        del justOrg["senders"]
    if "receivers" in justOrg:
        del justOrg["receivers"]
    print(f'PUT: {json.dumps(justOrg)}')
    url = f'{base_url}/settings/organizations/{org["name"]}'
    r = requests.put(url, data=json.dumps(justOrg), headers=headers)
    r.raise_for_status()


def put_sender(sender):
    print(f'PUT: {json.dumps(sender)}')
    url = f'{base_url}/settings/organizations/{sender["organizationName"]}/senders/{sender["name"]}'
    r = requests.put(url, data=json.dumps(sender), headers=headers)
    r.raise_for_status()


def put_receiver(receiver):
    print(f'PUT: {json.dumps(receiver)}')
    url = f'{base_url}/settings/organizations/{receiver["organizationName"]}/receivers/{receiver["name"]}'
    r = requests.put(url, data=json.dumps(receiver), headers=headers)
    r.raise_for_status()


def get_last_modified():
    url = f'{base_url}/settings/organizations'
    r = requests.head(url, headers=headers)
    r.raise_for_status()
    if 'Last-Modified' in r.headers:
        return parsedate_to_datetime(r.headers['Last-Modified'])
    else:
        return datetime.datetime(2000, 1, 1)  # early date to make logic work

# Check and wait for the API to be available for a number of retries
def waitForApiAvailability():
    print("Checking if the API is available at " + base_url)
    url = f'{base_url}/settings/organizations'
    retryInterval = 5
    maxRetries = args.conn_retries
    retryCount = 0
    connected = False
    while (connected is False) and (retryCount < maxRetries):
        try:
            requests.head(url, headers=headers)
            connected = True
            print("API is available.  Continuing...")
        except Exception as err:
            print("Unable to connect to the API.  Retrying " + str(maxRetries - retryCount) + " more times...")
            retryCount += 1
            time.sleep(retryInterval)
    if connected is False:
        print("ERROR: Unable to connect to API after " + str(retryCount) + " retries")
        sys.exit(1)

def main():
    try:
        waitForApiAvailability()

        if args.check_last_modified:
            settings_modified = get_last_modified()
            file_modified = os.path.getmtime(org_file)
            if settings_modified.timestamp() >= file_modified:
                print(f"Settings modified {settings_modified} after input file, do not update settings")
                return

        print(f"Loading {org_file} into {base_url}")
        orgs = parse_file(org_file)
        for org in orgs:
            put_org(org)
        for org in orgs:
            for sender in (org["senders"] if "senders" in org else []):
                put_sender(sender)
        for org in orgs:
            for receiver in (org["receivers"] if "receivers" in org else []):
                put_receiver(receiver)
    except Exception as err:
        print(err)


if __name__ == "__main__":
    main()
