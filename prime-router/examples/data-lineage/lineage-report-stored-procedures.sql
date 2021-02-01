-- Stored procedures that do cool lineage queries on report_files

DROP FUNCTION IF EXISTS report_descendants;
DROP FUNCTION IF EXISTS report_ancestors;
DROP FUNCTION IF EXISTS find_sent_reports;
DROP FUNCTION IF EXISTS find_withered_reports;

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
      AND A.action_name = 'send';
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
-- These are nodes that withered and died.
-- (As long as reports have relatively few descendants, the "NOT IN" query shouldn't be too expensive)
SELECT RF.report_id FROM tmp, action AS A, report_file AS RF
WHERE tmp.tmp_report_id = RF.report_id
      AND A.action_id = RF.action_id
      AND A.action_name != 'send'
      AND RF.report_id NOT IN 
            (select RL.parent_report_id from report_lineage RL where RL.parent_report_id = tmp.tmp_report_id);
END;
$$  LANGUAGE PLPGSQL;

