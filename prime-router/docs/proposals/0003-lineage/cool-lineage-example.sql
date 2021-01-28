-- Steps to use this as a demo (a hack, I know)

-- 1.  Run psql prime_data_hub -f ./docs/proposals/0003-lineage/lineage-report-stored-procedures.sql
--  This creates the stored procedures that do lineage queries.

-- 2. Then run this twice in quick succession (to force a merge of reports, which is a more interesting demo):
-- curl -X POST -H "client:simple_report" -H "Content-Type: text/csv"  --data-binary "@./src/test/csv_test_files/input/simplereport.csv""http://localhost:7071/api/reports"

-- 3. Then look at the respone json, and grab either one of the "id"s,
--      and use that REPORT_ID in the first three queries, below, eg: 'f0d85c32-266c-4a18-9400-ba56f17ef03a'
-- 4. Then run that first query, below.
-- Looking at the result of the first query, find the line that says 'send', 'CSV',  to 'az-phd',
--      and use that REPORT_ID in the _last_ query, below, eg, ('601a1858-4174-4422-a969-e28ec75b4ba3'))
--     That query works "in reverse" - that's why you need to find a child report, to make it interesting.
--
-- 5.  Finally, run the whole thing, below:
--     psql prime_data_hub -f  ./docs/proposals/0003-lineage/cool-lineage-example.sql

\echo >>>>> Find the entire lineage, starting from a simple-report
select RF.report_id, A.action_name as creating_action,
       RF.body_format, RF.sending_org, RF.receiving_org, left(RF.transport_result, 20) as send_result, RF.item_count
from report_file as RF
join action as A ON A.action_id = RF.action_id
where RF.report_id in (select report_descendants('f0d85c32-266c-4a18-9400-ba56f17ef03a'))
order by A.action_id, RF.sending_org;

\echo >>>>> Find the reports that were sent, based on a submitted simple_report
select RF.report_id, A.action_name as creating_action,
       RF.body_format, RF.sending_org, RF.receiving_org, left(RF.transport_result, 20) as send_result, RF.item_count
from report_file as RF
join action as A ON A.action_id = RF.action_id
where RF.report_id in (select find_sent_reports('f0d85c32-266c-4a18-9400-ba56f17ef03a'))
order by A.action_id, RF.sending_org;

\echo >>>>> Find the reports that were NEVER sent, based on a submitted simple_report
select RF.report_id, A.action_name as creating_action,
       RF.body_format, RF.sending_org, RF.receiving_org, left(RF.transport_result, 20) as send_result, RF.item_count
from report_file as RF
join action as A ON A.action_id = RF.action_id
where RF.report_id in (select find_withered_reports('f0d85c32-266c-4a18-9400-ba56f17ef03a'))
order by A.action_id, RF.sending_org;

\echo >>>>> Example query that finds the ancestors of a report that is the product of a merge
select RF.report_id, A.action_name as creating_action,
       RF.body_format, RF.sending_org, RF.receiving_org, left(RF.transport_result, 20) as send_result, RF.item_count
from report_file as RF
join action as A ON A.action_id = RF.action_id
where RF.report_id in (select report_ancestors('601a1858-4174-4422-a969-e28ec75b4ba3'))
order by A.action_id desc;





