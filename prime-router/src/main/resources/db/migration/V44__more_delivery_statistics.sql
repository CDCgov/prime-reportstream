/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 *
 * Copy a version of this comment into the next migration
 */

/*
 * Adds a new column to the report_file table
 * To support a specific delivery statistics reporting requirement.
 * See Report.kt::itemCountBeforeQualityFilter for full documentation.
 * 
 * This will be set for reports created by the routing/filtering step.
 * This will be null for all prior and subsequent steps, because it makes no sense elsewhere.
 */
ALTER TABLE report_file ADD COLUMN item_count_before_qual_filter INT;  -- null is ok.


/*
   SPECIAL_DELIVERY_STATISTICS FUNCTION

   This answers the question:   For every submission in the data range, please list the filtering and delivery statistics
   per submission per receiver.

   If you pass NULL for sender, this will run across all senders in the date range.
   If you pass in a sender name, this will run for just that one sender.

   This includes  all receivers, even those not working.
   This includes downloads.

   This query cannot distinguish between a failure in the process step, and a filtering action.
   (Filters are not failures or errors.)

   In PROD this will return dubious results if run with a date less
   than 24 hours ago, because the data may not have gone to the STLTs
   yet.

   Example calls.
   SELECT * FROM special_delivery_details('simple_report', '2022-02-04 00:00:00', '2022-02-05 00:00:00');
   SELECT * FROM special_delivery_details('strac', (now() - interval '10 minutes')::timestamp, now()::timestamp);
   SELECT * FROM special_delivery_details(null, (now() - interval '49 hours')::timestamp, (now() - interval '25 hours')::timestamp);
   SELECT * FROM special_delivery_details({{sender}}::text, {{starttime}}::timestamp, {{endtime}}::timestamp);   -- parameterized for Metabase!
   SELECT * FROM special_delivery_details(null, to_date('202212', 'iyyyiw')::timestamp,  (to_date('202212', 'iyyyiw') + 7)::timestamp);
*/
DROP FUNCTION IF EXISTS special_delivery_details;
CREATE OR REPLACE FUNCTION special_delivery_details(sender TEXT, start_time TIMESTAMP, end_time TIMESTAMP)
RETURNS TABLE
    (
     submitted_report_id UUID,
     filtered_report_id UUID,
     sending_org text,
     sending_org_client text,
     receiving_org text,
     receiving_org_svc text,
     count_submitted_for_jurisdiction int,
     count_after_filtering int,
     count_sent int,
     downloaded_by text
    ) AS
$$
BEGIN
RETURN QUERY 
WITH

-- This temp table has all the reports submitted during the query period
submitted_reports as (
select RF.report_id as submitted_report_id, RF.sending_org, RF.sending_org_client
from report_file RF
where
--   RF.sending_org = sender and 
--   RF.report_id = my_report_id
--   RF.created_at > '2022-03-01 00:00:00' and RF.created_at < '2022-04-10 00:00:00'
--   RF.report_id in ('ec56fc3e-a533-47ad-97cb-dece0885afe9',  -- this one was async
--                      'dc6bb29d-1385-4841-bd10-8cbab50f6ae6')  -- this one was sync
     RF.created_at > start_time and RF.created_at < end_time
     and
         -- sender is static at this point -- does the query planner have the sense to collapse this expr down?
         ((sender is NOT NULL and RF.sending_org = sender) -- if a sender param is passed in, use it to filter
	 OR
          (sender is NULL     and RF.sending_org is NOT NULL)) -- otherwise get for all senders
),


-- This temp table has all the post-routing/filtering Reports that are children of submitted_reports via
-- processing by the **SYNCHRONOUS** pipeline.
--
-- Since this is synchronous, the 'receive' step does the routing, so only one hop in report_lineage.
sync_reports_post_filtering as (
select
   SR.submitted_report_id,
   SR.sending_org, SR.sending_org_client,
   RF1.report_id filtered_report_id, RF1.receiving_org, RF1.receiving_org_svc,
   RF1.item_count_before_qual_filter, RF1.item_count
from submitted_reports SR
-- RL1 is the lineage from the submitted report to the child report post-routing/filtering
join report_lineage RL1 on SR.submitted_report_id = RL1.parent_report_id
join report_file RF1 on RL1.child_report_id = RF1.report_id
join action A1 on RF1.action_id = A1.action_id
where
   A1.action_name = 'receive'
   and RF1.receiving_org is not null
   and RF1.item_count_before_qual_filter is not null
),

-- And this temp table has all the post-routing/filtering Reports that are children of submitted_reports via
-- processing by the **ASYNCHRONOUS** pipeline.
--
-- Since this is async, the 'process' step does the routing, so we need to go two hops down in report_lineage.
async_reports_post_filtering as (
select
   SR.submitted_report_id,
   SR.sending_org, SR.sending_org_client,
   RF2.report_id filtered_report_id, RF2.receiving_org, RF2.receiving_org_svc,
   RF2.item_count_before_qual_filter, RF2.item_count
from submitted_reports SR
-- RL1 is the lineage from the submitted report to the child report ready for processing
join report_lineage RL1 on SR.submitted_report_id = RL1.parent_report_id
-- RL2 is the lineage from the report ready for processing to the child report post-routing/filtering
join report_lineage RL2 on RL1.child_report_id = RL2.parent_report_id
join report_file RF2 on RL2.child_report_id = RF2.report_id
join action A2 on RF2.action_id = A2.action_id
where
   A2.action_name = 'process'
   and RF2.receiving_org is not null
   and RF2.item_count_before_qual_filter is not null
),


all_reports_post_filtering as (
  select * from async_reports_post_filtering
  UNION
  select * from sync_reports_post_filtering
),

--    Example output of all_reports_post_filtering, from two reports, one sync and one async.
--    You can't tell which is sync/async --- which is the whole point - we've washed that out of the stats.
--          submitted_report_id          |  sending_org  | sending_org_client |          filtered_report_id          | receiving_org | receiving_org_svc | item_count_before_qual_filter | item_count
-- --------------------------------------+---------------+--------------------+--------------------------------------+---------------+-------------------+-------------------------------+------------
--  54133b3f-704c-4c20-9561-4061510cd70b | simple_report | default            | 7b0778d1-8c24-43c9-87cd-2be4b2ee5f3d | oh-doh        | elr               |                             2 |          0
--  54133b3f-704c-4c20-9561-4061510cd70b | simple_report | default            | 0f288023-a23c-4701-9373-5da2f31346ef | md-phd        | elr               |                             2 |          2
--  8fb0d0b0-412e-4489-89ef-e86c873ec159 | simple_report | default            | fbd20324-d781-47fc-8338-3f69d56c2019 | ak-phd        | elr               |                             5 |          2
--  54133b3f-704c-4c20-9561-4061510cd70b | simple_report | default            | 0ab9ef6a-c6da-4377-bbb5-1dac651225c6 | hi-phd        | elr               |                             5 |          2
--  54133b3f-704c-4c20-9561-4061510cd70b | simple_report | default            | 73c0c852-c7c7-4933-8249-7f5cdb2beea4 | ak-phd        | elr               |                             5 |          2
--  8fb0d0b0-412e-4489-89ef-e86c873ec159 | simple_report | default            | 03a09591-0f8b-48f1-b5be-2b33ecd52d44 | md-phd        | elr               |                             2 |          2
--  8fb0d0b0-412e-4489-89ef-e86c873ec159 | simple_report | default            | ef241c41-3837-4b27-9da3-98ea5b203a98 | oh-doh        | elr               |                             2 |          0
--  8fb0d0b0-412e-4489-89ef-e86c873ec159 | simple_report | default            | 07e4f566-2fa3-4345-8edc-ac4d580a2e20 | hi-phd        | elr               |                             5 |          2


-- Now we're ready to look at batch and send steps.
--
-- This temp table starts with all_reports_post_filtering, and follows the item lineage two steps down:
-- (post-filtering-reports) -> batch -> (batched-reports) -> send -> (sent-reports)
--
-- Note it is common for reports with no items in all_reports_post_filtering.  These will NOT appear as children here,
-- since you can't batch zero things!
--
-- At this point we have to switch to query by item_lineage instead of report_lineage,
-- because batched reports' item_count combines data from many different senders.
--
-- Include downloaded reports (which have a non-null 'downloaded_by').
--
-- Note that the batch step splits some HL7 deliveries into individual files.  The 'group by' here collapses them together properly,
-- but at the cost that we can't report on individual sent report_ids.
data_about_sent_reports as (
select
   ARPF.submitted_report_id,
   ARPF.filtered_report_id,
   ARPF.sending_org,
   ARPF.sending_org_client,
   RF4.receiving_org,
   RF4.receiving_org_svc,
   count(*) as sent_item_count,
   RF4.downloaded_by
from all_reports_post_filtering ARPF
-- IL3.parent_report_id is the post-routing/filtering report , and IL3.child_report_id is the batched report.
-- That is, this IL3 is lineage from (items entering batch) to (items leaving batch and ready to send).
join item_lineage IL3 on ARPF.filtered_report_id = IL3.parent_report_id
-- IL4.parent_report_id is the batched report, and IL4.child_report_id is the sent report.
-- That is, this IL4 is lineage from (items ready to send) to (items that have been sent).
join item_lineage IL4 on IL3.child_report_id = IL4.parent_report_id AND IL3.child_index = IL4.parent_index
-- Now join the IL4 sent items back to the report_file table:
join report_file RF4 on RF4.report_id = IL4.child_report_id
group by
   ARPF.submitted_report_id,
   ARPF.filtered_report_id,
   ARPF.sending_org, ARPF.sending_org_client,
   RF4.receiving_org, RF4.receiving_org_svc,
   RF4.downloaded_by
)

--  Example output of the above data_about_sent_reports query
--          submitted_report_id          |          filtered_report_id          |  sending_org  | sending_org_client | receiving_org | receiving_org_svc | sent_item_count | downloaded_by
-- --------------------------------------+--------------------------------------+---------------+--------------------+---------------+-------------------+-----------------+---------------
--  8fb0d0b0-412e-4489-89ef-e86c873ec159 | 07e4f566-2fa3-4345-8edc-ac4d580a2e20 | simple_report | default            | hi-phd        | elr               |               2 |
--  8fb0d0b0-412e-4489-89ef-e86c873ec159 | fbd20324-d781-47fc-8338-3f69d56c2019 | simple_report | default            | ak-phd        | elr               |               2 |
--  54133b3f-704c-4c20-9561-4061510cd70b | 0f288023-a23c-4701-9373-5da2f31346ef | simple_report | default            | md-phd        | elr               |               2 |
--  54133b3f-704c-4c20-9561-4061510cd70b | 73c0c852-c7c7-4933-8249-7f5cdb2beea4 | simple_report | default            | ak-phd        | elr               |               2 |
--  8fb0d0b0-412e-4489-89ef-e86c873ec159 | 03a09591-0f8b-48f1-b5be-2b33ecd52d44 | simple_report | default            | md-phd        | elr               |               2 |
--  54133b3f-704c-4c20-9561-4061510cd70b | 0ab9ef6a-c6da-4377-bbb5-1dac651225c6 | simple_report | default            | hi-phd        | elr               |               2 |


select
       ARPF.submitted_report_id,
       ARPF.filtered_report_id,
       ARPF.sending_org::text,
       ARPF.sending_org_client::text,
       ARPF.receiving_org::text,
       ARPF.receiving_org_svc::text,
       ARPF.item_count_before_qual_filter   as count_submitted_for_jurisdiction,
       ARPF.item_count                      as count_after_filtering,
       coalesce(SR.sent_item_count, 0)::int as count_sent, -- coalesce returns the first non-null value in list.
       SR.downloaded_by::text
from all_reports_post_filtering ARPF
-- left join includes rows (in ARPF) where nothing was sent (0 in SR)
left join data_about_sent_reports SR on ARPF.submitted_report_id = SR.submitted_report_id and ARPF.filtered_report_id = SR.filtered_report_id
order by sending_org, sending_org_client, submitted_report_id, receiving_org, receiving_org_svc;

-- Example output of the above query
--          submitted_report_id          |          filtered_report_id          |  sending_org  | sending_org_client | receiving_org | receiving_org_svc | count_submitted_for_jurisdiction | count_after_filtering | count_sent | downloaded_by
-- --------------------------------------+--------------------------------------+---------------+--------------------+---------------+-------------------+----------------------------------+-----------------------+------------+---------------
--  54133b3f-704c-4c20-9561-4061510cd70b | 73c0c852-c7c7-4933-8249-7f5cdb2beea4 | simple_report | default            | ak-phd        | elr               |                                5 |                     2 |          2 |
--  54133b3f-704c-4c20-9561-4061510cd70b | 0ab9ef6a-c6da-4377-bbb5-1dac651225c6 | simple_report | default            | hi-phd        | elr               |                                5 |                     2 |          2 |
--  54133b3f-704c-4c20-9561-4061510cd70b | 0f288023-a23c-4701-9373-5da2f31346ef | simple_report | default            | md-phd        | elr               |                                2 |                     2 |          2 |
--  54133b3f-704c-4c20-9561-4061510cd70b | 7b0778d1-8c24-43c9-87cd-2be4b2ee5f3d | simple_report | default            | oh-doh        | elr               |                                2 |                     0 |          0 |
--  8fb0d0b0-412e-4489-89ef-e86c873ec159 | fbd20324-d781-47fc-8338-3f69d56c2019 | simple_report | default            | ak-phd        | elr               |                                5 |                     2 |          2 |
--  8fb0d0b0-412e-4489-89ef-e86c873ec159 | 07e4f566-2fa3-4345-8edc-ac4d580a2e20 | simple_report | default            | hi-phd        | elr               |                                5 |                     2 |          2 |
--  8fb0d0b0-412e-4489-89ef-e86c873ec159 | 03a09591-0f8b-48f1-b5be-2b33ecd52d44 | simple_report | default            | md-phd        | elr               |                                2 |                     2 |          2 |
--  8fb0d0b0-412e-4489-89ef-e86c873ec159 | ef241c41-3837-4b27-9da3-98ea5b203a98 | simple_report | default            | oh-doh        | elr               |                                2 |                     0 |          0 |

END;
$$
LANGUAGE PLPGSQL;





/*
   SPECIAL_DELIVERY_STATISTICS FUNCTION

   This answers the question: FOR ALL SENDERS, if a sender sent data for a particular jurisdiction,
   did it pass filtering, and did we actually send it?
   
   This lists all receivers, even those not working, so you have to look at the 'is_actually_receiving' boolean.

   This query cannot distinguish between a failure in the process step, and a filtering action.
   (Filters are not failures or errors.)

   In PROD this will return dubious results if run with a date less
   than 24 hours ago, because the data may not have gone to the STLTs
   yet.

   Example calls.
   SELECT * FROM special_delivery_statistics('2022-02-04 00:00:00', '2022-02-05 00:00:00');
   SELECT * FROM special_delivery_statistics((now() - interval '10 minutes')::timestamp, now()::timestamp);
   SELECT * FROM special_delivery_statistics((now() - interval '49 hours')::timestamp, (now() - interval '25 hours')::timestamp);
   SELECT * FROM special_delivery_statistics({{starttime}}::timestamp, {{endtime}}::timestamp);   -- parameterized for Metabase!
   SELECT * FROM special_delivery_statistics(to_date('202212', 'iyyyiw')::timestamp,  (to_date('202212', 'iyyyiw') + 7)::timestamp);
*/
DROP FUNCTION IF EXISTS special_delivery_statistics;
CREATE OR REPLACE FUNCTION special_delivery_statistics(start_time TIMESTAMP, end_time TIMESTAMP)
--- sender TEXT, my_report_id UUID)
RETURNS TABLE
    (
     sending_org text,
     sending_org_client text,
     receiving_org text,
     receiving_org_svc text,
     sum_intended_for_jurisdiction int,
     sum_after_filtering int,
     sum_sent int,
     is_actually_receiving boolean
    ) AS
$$
BEGIN
RETURN QUERY 
WITH

special_delivery_details as (
SELECT * FROM special_delivery_details(null, start_time, end_time)
),
-- An aside, unrelated to the above flow of tables:
-- This temp table has all receivers who are active and have a transport defined.
-- 'is_actually_receiving' is true iff:
--    - The receiver is not deleted
--    - The receiver has a defined transport in the settings (eg, sftp, or other)
--    - The receiver has a defined batching time in the settings
--    - The receiver has an active customerStatus.
active_receiver as (
select setting.values ->> 'organizationName' as org_name, setting.values ->> 'name' as org_svc,
     ((setting.values ->> 'customerStatus' = 'active') and
      (setting.is_active = true) and
      (setting.is_deleted = false) and
      (setting.values -> 'transport' != 'null') and
      (setting.values -> 'timing' != 'null'))
     as is_actually_receiving
    from setting
    where setting.type = 'RECEIVER'
)

-- OK, finally, we have all the stats we want...
-- The glorious finale
select
       SDD.sending_org,
       SDD.sending_org_client,
       SDD.receiving_org,
       SDD.receiving_org_svc,
       sum(SDD.count_submitted_for_jurisdiction)::int as sum_intended_for_jurisdiction,
       sum(SDD.count_after_filtering)::int            as sum_after_filtering,
       sum(SDD.count_sent)::int                             as sum_sent, 
       AR.is_actually_receiving
from special_delivery_details SDD
join active_receiver AR on SDD.receiving_org = AR.org_name and SDD.receiving_org_svc = AR.org_svc
where downloaded_by is null  -- Do not include downloads, which can happen many times before its death; messes up the numbers.
group by
       SDD.sending_org,
       SDD.sending_org_client,
       SDD.receiving_org,
       SDD.receiving_org_svc,
       AR.is_actually_receiving;

-- Example result:
--   sending_org  | sending_org_client | receiving_org | receiving_org_svc | sum_intended_for_jurisdiction | sum_after_filtering | sum_sent | is_actually_receiving
-- ---------------+--------------------+---------------+-------------------+-------------------------------+---------------------+----------+-----------------------
--  simple_report | default            | ak-phd        | elr               |                            10 |                   4 |        4 | t
--  simple_report | default            | hi-phd        | elr               |                            10 |                   4 |        4 | f
--  simple_report | default            | md-phd        | elr               |                             4 |                   4 |        4 | t
--  simple_report | default            | oh-doh        | elr               |                             4 |                   0 |        0 | t

END;
$$
LANGUAGE PLPGSQL;
