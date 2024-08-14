/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 *
 * Copy a version of this comment into the next migration
 */


/*
   SPECIAL_DELIVERY_STATISTICS FUNCTION - FIXED.
  (The active_receiver query had a flaw in it in V44__*.sql - now fixed)

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

-- This temp table has all receivers who are active and have a transport defined.
-- 'is_actually_receiving' is true iff:
--    - The receiver is not deleted from the settings table
--    - The receiver has a defined transport in the settings (eg, sftp, or other)
--    - The receiver has a defined batching time in the settings
--    - The receiver has an active customerStatus.
active_receiver as (
select setting.values ->> 'organizationName' as org_name, setting.values ->> 'name' as org_svc,
     ((setting.values ->> 'customerStatus' = 'active') and
      (setting.values -> 'transport' != 'null') and
      (setting.values -> 'timing' != 'null'))
     as is_actually_receiving
    from setting
    where setting.type = 'RECEIVER' and
      (setting.is_active = true) and
      (setting.is_deleted = false)
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
