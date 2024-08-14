#!/usr/bin/env python3
import csv

# Convert the national ELR flat file into a schema YAML
def main():
    with open('pa_flatfile.csv') as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        line_count = 0
        for row in csv_reader:
            if line_count == 0:
                for col in row:
                    print(f'- name: {col}')
                    print(f'  csvField: {col}')
                    print(f'')
            line_count += 1

if __name__ == "__main__":
    main()
