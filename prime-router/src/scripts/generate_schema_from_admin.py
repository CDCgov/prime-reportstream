#!/usr/bin/env python3
import csv

def main():
    with open('NatELRFlatFile_Admin.csv') as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        line_count = 0
        for row in csv_reader:
            if line_count > 0:
                if row[3] == '':
                    break
                print(f'- name: {row[3]}')
                print(f'  hl7_field: {row[4]}')
                print(f'  hl7_operation: {row[5]}')
                print(f'  hl7_validation: {row[6]}')
                print(f'  hl7_template: {"~" if row[7] == "" else row[7]}')
            line_count += 1

if __name__ == "__main__":
    main()
