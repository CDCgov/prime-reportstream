#!/usr/bin/env python3
from time import sleep, strftime, gmtime
from faker import Faker
import random
import datetime

faker = Faker()


def generate_header(file):
    file.write("lab,first_name,last_name,state,test_time,specimen_id,observation\n")


def generate_row(file, lab):
    now_str = datetime.datetime.utcnow().replace(tzinfo=datetime.timezone.utc, microsecond=0).isoformat()
    ident = random.randint(10000000, 99999999)
    file.write(
        f'{lab},{faker.first_name()},{faker.last_name()},{random.choice(["AZ", "FL"])},{now_str},{ident},{random.choice(["covid-19:pos", "covid-19:neg"])}\n')


def generate_file(location, lab):
    f = open(location, "a")
    generate_header(f)
    for i in range(random.randint(2, 9)):
        generate_row(f, lab)
    f.close()


def main():
    t = strftime("%H-%M-%S", gmtime())
    generate_file("test_files/lab1-test_results-" + t + ".csv", 'lab1')
    generate_file("test_files/lab2-test_results-" + t + ".csv", 'lab2')


if __name__ == "__main__":
    main()
