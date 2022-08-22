/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

/*
 * This file contains two functions:
 * delivery_statistics and delivery_statistics_for_receiver.
 * They are documented below
 */


/*
   DELIVERY_STATISTICS FUNCTION

   This answers the question: if we intended to send data, did we
   actually do it?  This only looks at receivers with
   customerStatus==active, and with a non-null transport. This
   examines the pipeline at the point where data is ready for the
   Batch step, *after* all qualityFiltering. So it won't notice
   receive or process failures.

   In PROD this will return dubious results if run with a date less
   than 24 hours ago, because the data may not have gone to the STLTs
   yet.

   Here are some example calls to the new stored function.

   1) See delivery statistics for one day in the past:

   SELECT * FROM delivery_statistics('2022-02-04 00:00:00', '2022-02-05 00:00:00');

   2) See delivery statistics for the last few minutes(useful for
   testing the function.  Run this during/after a smoke test
   completes, to watch progress):

   SELECT * FROM delivery_statistics((now() - interval '10 minutes')::timestamp, now()::timestamp);

   3) See data from the previous 24 hours period (old enough that
   everything ought to have completed):

   SELECT * FROM delivery_statistics((now() - interval '49 hours')::timestamp, (now() - interval '25 hours')::timestamp);

   4) Use this version if you want to have input variables:

   SELECT * FROM delivery_statistics({{starttime}}::timestamp, {{endtime}}::timestamp);

   5) Get data for the 12th week of 2022: (Monday 2022-03-21 00:00:00
   to Monday 2022-03-28 00:00:00) Remember to change the week number
   in both places!  Yeah, I said 'iyyyiw' too when I saw this.

   SELECT  * FROM delivery_statistics(to_date('202212', 'iyyyiw')::timestamp,  (to_date('202212', 'iyyyiw') + 7)::timestamp);
*/
DROP FUNCTION IF EXISTS delivery_statistics;
CREATE OR REPLACE FUNCTION delivery_statistics(start_time TIMESTAMP, end_time TIMESTAMP)
RETURNS TABLE
    (
     receiving_org TEXT,
     receiving_org_svc TEXT,
     expected_count bigint,
     actual_count bigint,
     failed_count bigint,
     sum_overall_expected_count bigint,       -- not properly normalized
     sum_overall_actual_count bigint,         -- not properly normalized
     overall_percent_successful numeric(5,2)  -- not properly normalized
    ) AS
$$
BEGIN
RETURN QUERY 
-- This temp table has all receivers who are active and have a transport defined.
WITH active_receiver as (
select setting.values ->> 'organizationName' as org_name, setting.values ->> 'name' as org_svc
    from setting
    where setting.type = 'RECEIVER'
    and setting.values ->> 'customerStatus' = 'active'
    and setting.is_active = true
    and setting.is_deleted = false
    and setting.values -> 'transport' != 'null'
),

-- This temp table lists all reports within a time frame that were ready to be batched.
-- Need to pick a date range at least 24 hrs+ in the past if you want to compare to what was sent.
prior_to_batch as (
select
   IL.item_lineage_id, IL.child_report_id as item_report_id, IL.child_index as item_index,
   RF.receiving_org, RF.receiving_org_svc
from report_file RF
join item_lineage IL on RF.report_id = IL.child_report_id
join active_receiver AR on RF.receiving_org = AR.org_name and RF.receiving_org_svc = AR.org_svc
where
   RF.created_at > start_time and RF.created_at < end_time
and next_action = 'batch'
),

-- This temp table starts with the above prior_to_batch list, and
-- follow the lineage two steps down: (prior-to-batch-items) -> batch -> (batched-items) -> send -> (sent-items)
sent_items as (
select
  IL3.item_lineage_id, IL3.child_report_id, IL3.child_index, RF3.receiving_org, RF3.receiving_org_svc
from prior_to_batch PTB  -- PTB is items entering batch step.
-- This IL2 joins from (items entering batch) to (items leaving batch and ready to send).
join item_lineage IL2 on PTB.item_report_id = IL2.parent_report_id AND PTB.item_index = IL2.parent_index
-- This IL3 joins from (items ready to send) to (items that have been sent).
join item_lineage IL3 on IL2.child_report_id = IL3.parent_report_id AND IL2.child_index = IL3.parent_index
-- Now join the IL3 sent items back to the report_file table:
join report_file RF3 on RF3.report_id = IL3.child_report_id
where RF3.downloaded_by is null  -- Do not include downloads, which can happen many times; messes up the numbers.
),

-- This temp table summarizes just the incoming/expected (prior-to-batch) stats, separately.
prior_to_batch_summary_stats as (
select count(PTB.item_lineage_id) as expected_count,
       PTB.receiving_org, PTB.receiving_org_svc
from prior_to_batch PTB
group by PTB.receiving_org, PTB.receiving_org_svc
),

-- This temp table summarizes just the outgoing actual (after-send) stats, separately.
sent_summary_stats as (
select count(SI.item_lineage_id) as actual_count,
       SI.receiving_org, SI.receiving_org_svc
from sent_items SI 
group by SI.receiving_org, SI.receiving_org_svc
)

-- OK, finally, we have all the stats we want...
-- Combine the incoming/expected (prior to batch) stats
-- with the outgoing (actually sent) stats, all in a single table.
--
-- This is ugly, but the overall summary stats for all STLTs are plopped in the same table.
select PTBSS.receiving_org::text,
       PTBSS.receiving_org_svc::text,
       PTBSS.expected_count,
       SSS.actual_count,
       (PTBSS.expected_count - SSS.actual_count) as failed_count,
       (sum(PTBSS.expected_count) over())::bigint as sum_all_expected_count,
       (sum(SSS.actual_count) over())::bigint as sum_all_actual_count,
       round((sum(SSS.actual_count) over()) * 100 / (sum(PTBSS.expected_count) over()), 2) as overall_percent_successful
from prior_to_batch_summary_stats PTBSS
-- left join includes 'expected' rows (in PTBSS) where nothing was sent (0 in SSS)
left join sent_summary_stats SSS
     on SSS.receiving_org = PTBSS.receiving_org and SSS.receiving_org_svc = PTBSS.receiving_org_svc
group by PTBSS.receiving_org, PTBSS.receiving_org_svc, PTBSS.expected_count, SSS.actual_count
order by PTBSS.receiving_org, PTBSS.receiving_org_svc, PTBSS.expected_count, SSS.actual_count;
END;
$$
LANGUAGE PLPGSQL;



/*
 DELIVERY_STATISTICS_FOR_RECEIVER FUNCTION

 This function is the same as above, but it returns all the rows
 destined to go to a receiver, rather than just a summary.  You can
 then filter further, for example.  The inner query gets all the
 batched reports ("distinct batched_report_id") rows destined for
 Illinois in the previous day that were not sent ("where
 sent_report_id is null").  Then the outer query is a bit of a hack to
 search the ACTION table for errors related to those
 batched_report_ids.

select A.created_at, A.action_id::text, A.action_name, A.action_params, RF.report_id, RF.item_count, A.action_result from action A, report_file RF where
A.created_at > now() - interval '49 hours' 
and A.created_at < now()
and A.action_params like '%' || report_id || '%'
and RF.report_id in (
    select distinct batched_report_id 
    from
     delivery_statistics_for_receiver(
     (now() - interval '49 hours')::timestamp, (now() - interval '25 hours')::timestamp,
     'il-phd', 'elr'
     )
     where sent_report_id is null
)
order by created_at desc;

*/
DROP FUNCTION  IF EXISTS delivery_statistics_for_receiver;
CREATE OR REPLACE FUNCTION delivery_statistics_for_receiver(
    start_time TIMESTAMP, end_time TIMESTAMP,
    rcving_org TEXT, rcving_org_svc TEXT
) RETURNS TABLE
    (
       processed_item_lineage_id BIGINT,     -- id of item prior to batching
       tracking_id TEXT,                     -- id of the item, as created by the sender.
       received_at TIMESTAMP WITH TIME ZONE, -- timestamp it was received/processed
       processed_report_id UUID,             -- report_id prior-to-batch (ptb) (that is, after process step)
       batched_at TIMESTAMP WITH TIME ZONE,  -- timestamp it was batched
       batched_report_id UUID,               -- report_id of the batched report file.
       sent_at TIMESTAMP WITH TIME ZONE,     -- timestamp it was sent or downloaded
       sent_report_id UUID,                  -- report_id of the sent report file
       sent_tracking_id TEXT,                -- sanity check.  Should always be same as tracking_id above
       receiving_org TEXT,
       receiving_org_svc TEXT
    ) AS
$$
BEGIN
RETURN QUERY 

-- This temp table lists all reports within a time frame that were ready to be batched.
-- Need to pick a date range at least 24 hrs+ in the past if you want to compare to what was sent.
with prior_to_batch as (
select
   IL.item_lineage_id,
   IL.child_report_id as item_report_id,
   IL.child_index as item_index,
   IL.tracking_id,
   IL.created_at as received_at, -- A slight cheat - assumes we process as soon as we receive
   RF.receiving_org, RF.receiving_org_svc
from report_file RF
join item_lineage IL on RF.report_id = IL.child_report_id
where
   RF.created_at > start_time and RF.created_at < end_time
and next_action = 'batch'
and RF.receiving_org = rcving_org and RF.receiving_org_svc = rcving_org_svc
)

-- Follow the lineage two steps down: (prior-to-batch-items) -> batch -> (batched-items) -> send -> (sent-items)
select
  PTB.item_lineage_id as processed_item_lineage_id, -- id of item prior to batching
  PTB.tracking_id::TEXT,                            -- id of the item, as created by the sender.
  PTB.received_at,                          -- timestamp it was received/processed
  PTB.item_report_id as processed_report_id,-- report_id prior-to-batch (ptb) (that is, after process step)
  IL2.created_at as batched_at,           -- timestamp it was batched
  IL2.child_report_id as batched_report_id, -- report_id of the batched report file.
  IL3.created_at as sent_at,              -- timestamp it was sent or downloaded
  IL3.child_report_id as sent_report_id,    -- report_id of the sent report file
  IL3.tracking_id::TEXT as sent_tracking_id,-- sanity check.  Should always be same as tracking_id above
  PTB.receiving_org::TEXT,
  PTB.receiving_org_svc::TEXT
from prior_to_batch PTB  -- PTB is items entering batch step.
-- This IL2 joins from (items entering batch) to (items leaving batch and ready to send).
left join item_lineage IL2 on PTB.item_report_id = IL2.parent_report_id AND PTB.item_index = IL2.parent_index
-- This IL3 joins from (items ready to send) to (items that have been sent).
left join item_lineage IL3 on IL2.child_report_id = IL3.parent_report_id AND IL2.child_index = IL3.parent_index
-- Now join the IL3 sent items back to the report_file table:
-- join report_file RF3 on RF3.report_id = IL3.child_report_id
-- Note: I have removed "where RF3.downloaded_by is null", so this query DOES include downloads.
;
END;
$$
LANGUAGE PLPGSQL;


