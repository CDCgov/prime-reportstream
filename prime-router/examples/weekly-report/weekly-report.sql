-- Note: these queries find all reports in the last 168 hours from the time the queries are run.
-- For a query that instead finds all reports in the previous day, UTC time, replace this part of each query:
--   and RF.created_at > now() - interval '7 days'
-- with:
--   and RF.created_at::date = 'yesterday'::date

\echo These reports only look at the outbound side of the Hub
\echo
\echo List of all downloads that occurred in the last week
select PARENT.report_id, PARENT.item_count, PARENT.receiving_org, PARENT.receiving_org_svc,
       date_trunc('second',PARENT.created_at) READY_AT,
       date_trunc('second', CHILD.created_at) DOWNLOADED, CHILD.downloaded_by, PARENT.body_format
from report_file PARENT
join report_lineage RL on PARENT.report_id = RL.parent_report_id
join report_file CHILD on CHILD.report_id = RL.child_report_id
join action A on A.action_id = CHILD.action_id
where 
  A.action_name = 'download'
  and CHILD.created_at > now() - interval '7 days'
order by receiving_org, receiving_org_svc, CHILD.created_at
;

\echo ALL Reports supposed to go out, from the last week
select report_id, item_count, receiving_org, receiving_org_svc, date_trunc('second',RF.created_at) CREATED,
       A.action_name, RF.next_action, RF.body_format
from report_file RF
join action A on A.action_id = RF.action_id
where 
  (A.action_name = 'batch' or RF.next_action = 'send')
  and RF.created_at > now() - interval '7 days'
order by receiving_org, receiving_org_svc, RF.created_at
;

\echo Of the last weeks reports ready to be sent, here are those that actually got sent or downloaded
\echo Careful: there will be repeats, eg, for multiple downloads of the same report
select PARENT.report_id, PARENT.item_count, PARENT.receiving_org, PARENT.receiving_org_svc,
       date_trunc('second', PARENT.created_at) CREATED,
       date_trunc('second', CHILD.created_at) SENT, CHILD.downloaded_by, PARENT.body_format
from report_file PARENT
join report_lineage RL on PARENT.report_id = RL.parent_report_id
join report_file CHILD on CHILD.report_id = RL.child_report_id
where PARENT.report_id in 
(
  -- this nested query is just the list of all reports supposed to go out in last week
  select report_id
  from report_file RF
  join action A on A.action_id = RF.action_id
  where 
    (A.action_name = 'batch' or RF.next_action = 'send')
    and RF.created_at > now() - interval '7 days'
)
order by receiving_org, receiving_org_svc, PARENT.created_at
;

\echo Of the last weeks reports ready to be sent, here are those that were NEVER sent or downloaded
select report_id, item_count, receiving_org, receiving_org_svc, date_trunc('second',RF.created_at) CREATED, RF.body_format
from report_file RF
where 
RF.report_id in 
(
  -- this nested query is just the list of all reports supposed to go out in last week
  select report_id
  from report_file RF2
  join action A on A.action_id = RF2.action_id
  where 
    (A.action_name = 'batch' or RF2.next_action = 'send')
    and RF2.created_at > now() - interval '7 days'
)
-- will this be crazy slow?
and RF.report_id not in (select parent_report_id from report_lineage)
order by receiving_org, receiving_org_svc, RF.created_at
;
