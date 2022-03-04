## Example Submission with Interesting Response

This example is meant to represent a variety of warning messages and filtering messages.

You can submit the simple_report_example.csv file, either locally or in staging.  The submission response should return a nice example json response that has a smattering of both whole-report-level and individual-item-level warnings and also some interesting filters too.

This example should not return any errors, because they prevent the report from going further.

### How to generate the interesting json response

Run your local reportstream.
Then, using your favority POSTal system, submit something like this:

```
curl -X POST -H "client:simple_report" -H "Content-Type: text/csv"  --data-binary "@./examples/submission/simple_report_example.csv" "http://localhost:7071/api/reports" | python -mjson.tool
```

Then you can grab the "id" from the json response, and submit it to the Histor API.  Here's an example History API request

```
curl -H "client:simple_report" -H "authorization:bearer 123" "http://localhost:7071/api/history/simple_report/report/d44b1d7c-2974-4663-a929-0ef83004b32f" | python -mjson.tool
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



