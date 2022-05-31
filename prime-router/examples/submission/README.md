## Example 1:  Submission with Interesting Response

This example is meant to represent a variety of warning messages and filtering messages.

You can submit the simple_report_example.csv file, either locally or in staging.  The submission response should return a nice example json response that has a smattering of both whole-report-level and individual-item-level warnings and also some interesting filters too.

This example should not return any errors, because they prevent the report from going further.

### How to generate the interesting json response

Run your local reportstream.
Then, using your favority POSTal system, submit something like this:

```
curl -X POST -H "client:simple_report" -H "Content-Type: text/csv"  --data-binary "@./examples/submission/simple_report_example.csv" "http://localhost:7071/api/reports"
```

Then you can grab the "id" from the json response, and submit it to the Histor API.  Here's an example History API request

```
curl "http://localhost:7071/api/waters/report/d44b1d7c-2974-4663-a929-0ef83004b32f/history"
```

### Structure of the simple_report_example.csv 

My initial basic design of this file:

- I'm using simple_report as our sample csv file
- There are four states: AK, HI, OH, MD.  Picked totally at random.
- I'm using the 5 rows of AK data to show Quality Filtering
- I'm using the 5 rows of HI data to show various warnings
- I'm using the 2 rows of OH to show a case where ALL the rows get filtered out.
- Since I think Maryland is a happy state, and has the coolest flag, I'm using the 2 rows of MD to show a happy path case with no warnings, and no filtering.
- Totally feel free to mess with this file to make it generate other interesting example json.  

## Example 2:  Example of a duplicate submission

This example is meant to show the errors you get when you submit duplicate rows.

### How to generate the json response

```
curl -X POST -H "client:ignore.no-duplicates" -H "Content-Type: text/csv"  --data-binary "@./examples/submission/duplicate.csv" "http://localhost:7071/api/reports"
```

You can also change a bit of data in one of the rows and resubmit the file again, as above.  This should result in a row being in error as well.


### Structure of the duplicate.csv 

There are two identical rows.


### How to generate the three .json example files

This assumes you have ReportStream running locally.  If you just started the Functions, you'll need to do these steps twice, because the startup is so slow, you won't be fast enough the first time.

#### Steps

Wait until just a second past the top of the minute, and run:

```
curl -X POST -H "client:simple_report" -H "Content-Type: text/csv"  --data-binary "@./examples/submission/simple_report_example.csv" "http://localhost:7071/api/reports?processing=async" > examples/submission/example1-async-response.json
```

Grab the submissionId

```
cat examples/submission/example1-async-response.json
```

Let's say the submissionId is 1211.  Quickly put it in this query, and run it before the minute expires:

```
curl "localhost:7071/api/waters/report/1211/history" > examples/submission/example2-sync-response.json
```

Now you can take your time.

Bonus:  Go into your the UI and do a download or two as 'md-phd' user, so that downloads show up in the example.

Wait another minute or longer (so the data flows all the way to sftp) and run this:

```
curl "localhost:7071/api/waters/report/1211/history" > examples/submission/example3-complete-response.json
```

Be sure to do a `git diff` to confirm that the changes you expected are in the files.    If not, you may need to run it again, because its just so slow the first time you run it.





