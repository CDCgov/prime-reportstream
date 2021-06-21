#!/bin/bash
for i in $(seq 1000)
do
    curl -X POST -H "Content-Type:text/csv"  --data-binary @result_files/fake-pdi-covid-19.csv 'http://172.17.0.1:7071/api/reports?client=simple_report'
    echo "Post $i times"
    sleep .5
done