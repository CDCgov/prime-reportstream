# Uploading Fake Data

1. Compile and Run PRIME ReportStream
2. Generate fake CSV data
3. Post CSV data to PRIME Web API
4. Examine

### Compile and Run PRIME ReportStream

```bash
# from the `prime-router/` directory

# run infrastructure like PostgreSQL
bash devenv-infrastructure.sh

# build and run PRIME ReportStream
docker compose up -d

# optionally, follow logs of `prime_dev` container
docker compose logs -f prime_dev
```

### Generate fake data

Use `prime data` command to generate fake data.

```bash
# from the `prime-router/` directory
./prime data --input-fake 23 \
  --input-schema primedatainput/pdi-covid-19 \
  --output examples/upload-fake-data/Test.csv
```

See `prime data --help` for details on the many options supported by the tool.
Notable support includes CSV and HL7 output format support, as well as transforms for schemas, formats, and receviers.


### Post CSV data to PRIME Web API

```bash
cd examples/upload-fake-data

# from the `prime-router/examples/upload-fake-data` directory
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.csv' \
  'http://localhost:7071/api/reports'
```

Example output:

```json
{
  "id" : "d3fb53b7-984e-49e0-8988-93918594cdd7",
  "reportItemCount" : 23,
  "destinations" : [
    "Sending 2 items to Arizona PHD (elr-test) at 2021-01-28T15:47-07:00",
    "Sending 2 items to Arizona PHD (elr-hl7-test) immediately",
    "Sending 2 items to Colorado Department of Health immediately",
    "Sending 1 items to Texas Department of State Health Services (elr-test) immediately",
    "Sending 1 items to Florida Department of Health immediately",
    "Sending 2 items to Pennsylvania Department of Health (elr-bucks-local) immediately",
    "Sending 3 items to Pennsylvania Department of Health (elr-chester-local) immediately",
    "Sending 2 items to Pennsylvania Department of Health (elr-montgomery-local) immediately"
  ],
  "warningCount" : 0,
  "errorCount" : 0,
  "errors" : [ ],
  "warnings" : [ ]
}
```

### Examine

View the local download page: [http://localhost:7071/api/download](http://localhost:7071/api/download)

Inspect the local devenv PostgreSQL database using `psql` or
using [`adminer`](http://localhost:8080/?pgsql=db_pgsql&username=prime&db=prime_data_hub&ns=public).
Connection details can be found in [docker-infrastructure.yml](../../docker-infrastructure.yml)
