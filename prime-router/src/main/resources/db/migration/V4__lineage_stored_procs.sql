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
 * This SQL makes some tweaks to report_file table,
 * and it adds basic stored procedures to do recursive lineage queries.
 */

ALTER TABLE report_file DROP CONSTRAINT report_file_external_name_key;
ALTER TABLE report_file ADD COLUMN downloaded_by VARCHAR(63);  -- null ok.

-- Stored procedures that do cool lineage queries on report_files

-- Find myself plus all my children, grandchildren, etc
CREATE OR REPLACE FUNCTION report_descendants(start_report_id UUID)
RETURNS SETOF UUID
AS $$
DECLARE
BEGIN
    RETURN QUERY
      WITH RECURSIVE tmp AS (
        SELECT start_report_id AS tmp_report_id
       UNION ALL
        SELECT RL.child_report_id
        FROM report_lineage AS RL
       JOIN tmp ON tmp.tmp_report_id = RL.parent_report_id
      )
SELECT tmp_report_id FROM tmp;
END;
$$  LANGUAGE PLPGSQL;

-- Find all reports sent or downloaded, that descended from me:
CREATE OR REPLACE FUNCTION find_sent_reports(start_report_id UUID)
RETURNS SETOF UUID
AS $$
DECLARE
BEGIN
    RETURN QUERY 
      WITH RECURSIVE tmp AS (
        SELECT report_descendants(start_report_id) AS tmp_report_id
      )
SELECT RF.report_id FROM tmp, action AS A, report_file AS RF
WHERE tmp.tmp_report_id = RF.report_id
      AND A.action_id = RF.action_id
      AND (A.action_name = 'send' OR A.action_name = 'download');
END;
$$  LANGUAGE PLPGSQL;

-- Find all things that descended from me, but did not get sent.  That is, things that withered and dead-ended.
-- Maybe these are errors that need to be addressed.
-- If this return no rows, that means all the data was sent.
-- Otherwise, this returns a list of the THINGs of the my descendants that withered and died.
CREATE OR REPLACE FUNCTION find_withered_reports(start_report_id UUID)
RETURNS SETOF UUID
AS $$
DECLARE
BEGIN
    RETURN QUERY 
      WITH RECURSIVE tmp AS (
        SELECT report_descendants(start_report_id) AS tmp_report_id
      )
-- Now find all reports that had no children and are not 'sent' leaf nodes.
-- These are nodes that withered and died.  Note:  a report with at least one download is considered 'sent' as well.
-- (As long as reports have relatively few descendants, the "NOT IN" query shouldn't be too expensive)
SELECT RF.report_id FROM tmp, action AS A, report_file AS RF
WHERE tmp.tmp_report_id = RF.report_id
      AND A.action_id = RF.action_id
      AND A.action_name != 'send' AND A.action_name != 'download'
      AND RF.report_id NOT IN 
            (select RL.parent_report_id from report_lineage RL where RL.parent_report_id = tmp.tmp_report_id);
END;
$$  LANGUAGE PLPGSQL;

-- Ancestor queries

-- Most basic: Find myself plus all my parents, grandparents, etc, back to the first Ur reports.
CREATE OR REPLACE FUNCTION report_ancestors(start_report_id UUID)
RETURNS SETOF UUID
AS $$
DECLARE
BEGIN
    RETURN QUERY
      WITH RECURSIVE tmp AS (
        SELECT start_report_id AS tmp_report_id
       UNION ALL
        SELECT RL.parent_report_id
        FROM report_lineage AS RL
       JOIN tmp ON tmp.tmp_report_id = RL.child_report_id
      )
SELECT tmp_report_id FROM tmp;
END;
$$  LANGUAGE PLPGSQL;

-- Find the externally submitted reports to the Hub that I'm descended from.
-- That's Ur as in Abram and Sarai, not "you are"  ;)
CREATE OR REPLACE FUNCTION report_ur_ancestors(start_report_id UUID)
RETURNS SETOF UUID
AS $$
DECLARE
BEGIN
    RETURN QUERY
      WITH RECURSIVE tmp AS (
        SELECT start_report_id AS tmp_report_id
       UNION ALL
        SELECT RL.parent_report_id
        FROM report_lineage AS RL
       JOIN tmp ON tmp.tmp_report_id = RL.child_report_id
      )
SELECT RF.report_id FROM tmp, action AS A, report_file AS RF
WHERE tmp.tmp_report_id = RF.report_id
      AND A.action_id = RF.action_id
      AND (A.action_name = 'receive')
      AND RF.sending_org is not null;
END;
$$  LANGUAGE PLPGSQL;


