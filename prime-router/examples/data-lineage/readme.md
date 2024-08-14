# Exploring Data Lineage

This example explores data lineage operations supported by the database design of PRIME ReportStream.

For more information about the design and genesis of these features, see [docs/proposals/0003-lineage/](../../docs/proposals/0003-lineage/).


### Setup DB and Example Data

- Creates fake data files for posting to API
- Posts fake data and captures API response into JSON files


```sh
bash ./example-setup.sh
```

Output files:
  - `Test.a.csv`
  - `Test.b.csv`
  - `Test.c.csv`
  - `Test-post.a.json`
  - `Test-post.b.json`
  - `Test-post.c.json`
  - `Test-post.bdup.json`


(See [./example-setup.sh](./example-setup.sh) for details.)


### Response JSON

Look at the response JSON files and choose one of the top-level "id" field values.

```json
{
  "id" : "f992d454-48fe-475f-b22d-633e0a67bddf",
  "reportItemCount" : 200,
  "destinations" : [
    "Sending 41 items to Arizona PHD (elr-test) at 2021-02-01T13:53-07:00",
    ...,
    "Sending 12 items to Pennsylvania Department of Health (elr-montgomery-local) immediately" ],
  "warningCount" : 0,
  "errorCount" : 0,
  "errors" : [ ],
  "warnings" : [ ]
}
```

Use that id as `REPORT_ID` in the first three queries below. (e.g. `f992d454-48fe-475f-b22d-633e0a67bddf`)
(Note that between the shell and `psql` commands, quoting of the id value can be tricky.)

```
REPORT_ID=$(jq -r ".id" ./Test-post.a.json)
```


### Query the full lineage

```sh
bash ./example-full.sh

# or, using `psql` from your command line:

psql prime_data_hub prime \
  -v "param_id=$REPORT_ID" \
  < ./lineage-full.sql
```

See [./lineage-full.sql](./lineage-full.sql)



### Query the sent reports

```sh
bash ./example-sent.sh

# or, using `psql` from your command line:

psql prime_data_hub prime \
  -v "param_id=$REPORT_ID" \
  < ./lineage-sent.sql
```

See [./lineage-sent.sql](./lineage-sent.sql)



### Query the reports never sent

```sh
bash ./example-never-sent.sh

# or, using `psql` from your command line:

psql prime_data_hub prime \
  -v "param_id=$REPORT_ID" \
  < ./lineage-never-sent.sql
```

See [./lineage-never-sent.sql](./lineage-never-sent.sql)



### Go backward! Query the ancestors

```sh
bash ./example-ancestors.sh

# or, using `psql` from your command line:

psql prime_data_hub prime \
  -v "param_id=$SENT_REPORT_ID" \
  < ./lineage-ancestors.sql
```

See [./lineage-ancestors.sql](./lineage-ancestors.sql)


### Go sideways! Query the cousins

```sh
bash ./example-cousins.sh

# or, using `psql` from your command line:

psql prime_data_hub prime \
  -v "param_id=$SENT_REPORT_ID" \
  < ./lineage-cousins.sql
```

See [./lineage-cousins.sql](./lineage-cousins.sql)
