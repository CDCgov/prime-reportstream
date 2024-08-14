#!/usr/bin/env python3
import csv

# Convert the national ELR flat file into a schema YAML
def main():
    with open('PDI_fields.csv') as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        line_count = 0
        for row in csv_reader:
            if line_count > 0:
                print(f'- name: {row[0]}')
                print(f'  csvField: {row[0]}')
                print(f'')
            line_count += 1

if __name__ == "__main__":
    main()
