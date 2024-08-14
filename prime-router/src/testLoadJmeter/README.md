# ai-apps-sdet-reportstream-load

The purpose of this script is to determine the number of API requests are submitted within a 2 minute period, while varying the number of threads. The results can be used for performance tuning.

For simplicity, this script has been stripped of extraneous features (e.g. reporting, graphs) and setup to run via command-line. 

To allow local vs staging environments, the required parameters are threads, protocol, server, and port:
'-Jthreads=1 -Jprotocol=https -Jserver=staging.prime.cdc.gov -Jport=443'

Prereq - Download and install JMeter
https://jmeter.apache.org/download_jmeter.cgi

1. Setup authorization key to be used for x-functions-key header. This script uses the variable "authkey" as the system environment variable.

    export authkey=<function key>

2. Via command-line, execute the test. The test will run for 2 minutes.

'jmeter -n -t DataRobot_ReportStream_Load.jmx -Jthreads=1 -Jprotocol=https -Jserver=staging.prime.cdc.gov -Jport=443 -l results.jtl'

3. The total API requests is part of stdout. Also, in the above example, the test results were captured in the results.jtl file. To get a quick api count, get the row count of the file (includes header-row):

'wc results.jtl'

NOTE: subsquent runs will append to the results file.


SAMPLE RUN

giang@ghoang-me-R865M:~/workspace/prime-data-hub/prime-router/src/testLoadJmeter$ jmeter -n -t DataRobot_ReportStream_Load.jmx -Jthreads=1 -Jprotocol=https -Jserver=staging.prime.cdc.gov -Jport=443 -l results.jtl
WARNING: package sun.awt.X11 not in java.desktop
Creating summariser <summary>
Created the tree successfully using DataRobot_ReportStream_Load.jmx
Starting standalone test @ Tue Jul 27 10:27:56 EDT 2021 (1627396076894)
Waiting for possible Shutdown/StopTestNow/HeapDump/ThreadDump message on port 4445
summary +      4 in 00:00:04 =    1.1/s Avg:   857 Min:   678 Max:  1308 Err:     0 (0.00%) Active: 1 Started: 1 Finished: 0
summary +     44 in 00:00:30 =    1.5/s Avg:   682 Min:   567 Max:   846 Err:     0 (0.00%) Active: 1 Started: 1 Finished: 0
summary =     48 in 00:00:34 =    1.4/s Avg:   697 Min:   567 Max:  1308 Err:     0 (0.00%)
summary +     42 in 00:00:30 =    1.4/s Avg:   707 Min:   587 Max:   933 Err:     0 (0.00%) Active: 1 Started: 1 Finished: 0
summary =     90 in 00:01:03 =    1.4/s Avg:   701 Min:   567 Max:  1308 Err:     0 (0.00%)
summary +     45 in 00:00:30 =    1.5/s Avg:   662 Min:   569 Max:   907 Err:     0 (0.00%) Active: 1 Started: 1 Finished: 0
summary =    135 in 00:01:33 =    1.4/s Avg:   688 Min:   567 Max:  1308 Err:     0 (0.00%)
summary +     32 in 00:00:27 =    1.2/s Avg:   844 Min:   597 Max:  4883 Err:     0 (0.00%) Active: 0 Started: 0 Finished: 0
summary =    167 in 00:02:00 =    1.4/s Avg:   718 Min:   567 Max:  4883 Err:     0 (0.00%)
Tidying up ...    @ Tue Jul 27 10:29:57 EDT 2021 (1627396197230)
... end of run
giang@ghoang-me-R865M:~/workspace/prime-data-hub/prime-router/src/testLoadJmeter$ wc results.jtl
     168    1838   35243 results.jtl


In the above run, there were no Err (monitor the stdout progress), and a total of 167 requests as indicated in stdout summary and wc.
