/*
The Flyway tool applies this migration to create the database.

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration

*/

/* 
 * This SQL creates tables to track fine-grained item-level lineage
 * It also makes some tweaks to report_file table,
 * and it adds basic stored procedures to do recursive lineage queries.
 */


ALTER TABLE report_file DROP CONSTRAINT report_file_external_name_key;

-- Each row is an item that's part of a report.
-- item_index is where the item can be found in the report, eg, item 1, item 2, etc.
CREATE TABLE item (
    item_id BIGSERIAL PRIMARY KEY,
    item_index INTEGER NOT NULL,   -- First actual data item in report is 1.  Not zero-based.
    report_id UUID NOT NULL REFERENCES report_file(report_id) ON DELETE CASCADE,
    tracking_id VARCHAR(128) NOT NULL,
    transport_params VARCHAR(512),
    transport_result VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX item_index_report_idx ON item(item_index, report_id);
CREATE INDEX item_tracking_id_idx ON item(tracking_id);


-- Each row represents a state transition in the data, caused by an action.
CREATE TABLE item_lineage (
    item_lineage_id BIGSERIAL PRIMARY KEY,
    action_id BIGINT NOT NULL REFERENCES action(action_id) ON DELETE CASCADE,
    parent_item_id BIGINT NOT NULL REFERENCES item(item_id) ON DELETE CASCADE,
    child_item_id BIGINT NOT NULL REFERENCES item(item_id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

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

-- Find myself plus all my parents, grandparents, etc, back to the first protoplasm
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

-- Find all reports sent, that descended from me:
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

