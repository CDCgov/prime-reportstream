-- This file is an experiment on meeting the primary use cases for Lineage Tracking.
-- You can run it locally like this:  psql prime_data_hub -f recursive-parent-child-test.sql 
--
-- It creates a couple simple lineage tracking tables, and populates them with a simple tree of lineage.
--
-- Then it creates a bunch of functions that are examples of our major use cases.
--  
-- Then it runs the functions on the tables, to prove that things are copacetic.

DROP FUNCTION IF EXISTS descendants;
DROP FUNCTION IF EXISTS ancestors;
DROP FUNCTION IF EXISTS find_sent;
DROP FUNCTION IF EXISTS find_withered;

DROP TABLE IF EXISTS thing_lineage;
DROP TABLE IF EXISTS thing;

-- These two tables, thing and thing_lineage, represent a very simple recursive parent/child schema.

CREATE TABLE thing (
    task varchar(20),   -- received, intermediate, sent
    thing_id INT PRIMARY KEY
);

create table thing_lineage (
    lineage_id INT PRIMARY KEY,
    parent_thing_id INT references thing(thing_id) ON DELETE CASCADE,
    child_thing_id INT references thing(thing_id) ON DELETE CASCADE
);

-- Populate a very simple tree of data, for experimentation and testing.

insert into thing values ('received', 100); -- parent to 200, 300, 400, 500.  300 is a withered child leaf.
insert into thing values ('intermediate', 200); -- parent to 400, 500
insert into thing values ('intermediate', 300); -- no children.  Child of 100.  Withered and Died without sending.
insert into thing values ('intermediate', 400); -- parent to 500
insert into thing values ('sent', 500); -- no children.  Child of 100, 200, 400, 600
insert into thing values ('received', 600); -- parent to 500
insert into thing values ('received', 700); -- no children, no parents.  Withered and Died without sending.

insert into thing_lineage values (1, 100, 200);
insert into thing_lineage values (2, 100, 300);
insert into thing_lineage values (3, 200, 400);
insert into thing_lineage values (4, 400, 500);
insert into thing_lineage values (5, 600, 500);  -- merge.  500 has two parents.


-- Find myself plus all my children
CREATE OR REPLACE FUNCTION descendants(start_id INT)
RETURNS SETOF INT
AS $$
DECLARE
BEGIN
    RETURN QUERY
      WITH RECURSIVE tmp AS (
        SELECT start_id AS thing_id
       UNION ALL
        SELECT TL.child_thing_id
        FROM thing_lineage AS TL
       JOIN tmp ON tmp.thing_id = TL.parent_thing_id
      )
SELECT thing_id FROM tmp;
END;
$$  LANGUAGE PLPGSQL;

-- Find all things with task 'sent' that descended from me:
CREATE OR REPLACE FUNCTION find_sent(start_id INT)
RETURNS SETOF INT
AS $$
DECLARE
BEGIN
    RETURN QUERY
      WITH RECURSIVE tmp AS (
        SELECT start_id AS thing_id
       UNION ALL
        SELECT TL.child_thing_id
        FROM thing_lineage AS TL
       JOIN tmp ON tmp.thing_id = TL.parent_thing_id
      )
SELECT thing.thing_id FROM tmp, thing
WHERE tmp.thing_id = thing.thing_id 
      AND thing.task = 'sent';
END;
$$  LANGUAGE PLPGSQL;

-- Find all things that descended from me, but did not get sent.  That is, things that withered and dead-ended.
-- Maybe these are errors that need to be addressed.
-- If this return no rows, that means all the data was sent.
-- Otherwise, this returns a list of the THINGs of the my descendants that withered and died.
CREATE OR REPLACE FUNCTION find_withered(start_id INT)
RETURNS SETOF THING
AS $$
DECLARE
BEGIN
    RETURN QUERY
      WITH RECURSIVE tmp AS (
        SELECT start_id AS thing_id
       UNION ALL
        SELECT TL.child_thing_id
        FROM thing_lineage AS TL
       JOIN tmp ON tmp.thing_id = TL.parent_thing_id
      )

-- Now TMP holds me, plus all my descendants 
-- Now look through TMP and find all nodes that had no children and are not 'sent' leaf nodes.
-- These are nodes that withered and died.
-- (As long as nodes have relatively few descendants, the "NOT IN" query shouldn't be too expensive)
select thing.*
from thing, tmp
where thing.thing_id = tmp.thing_id
  and thing.thing_id NOT IN
            (select TL.parent_thing_id from thing_lineage TL where TL.parent_thing_id = thing.thing_id)
  and thing.task != 'sent';

END;
$$  LANGUAGE PLPGSQL;

-- Find myself plus all parents
CREATE OR REPLACE FUNCTION ancestors(start_id INT)
RETURNS SETOF INT
AS $$
DECLARE
BEGIN
    RETURN QUERY
      WITH RECURSIVE tmp AS (
        SELECT start_id AS thing_id
       UNION ALL
        SELECT TL.parent_thing_id
        FROM thing_lineage AS TL
       JOIN tmp ON tmp.thing_id = TL.child_thing_id
      )
SELECT thing_id FROM tmp;
END;
$$  LANGUAGE PLPGSQL;

-- Now, finally, we can test the recursive select in various ways:

\echo Find all the descendants of each node, including the original parent node.

\echo Descendants of 100 are 100, 200, 300, 400, 500
select descendants(100);

\echo Descendants of 200 are 200, 400, 500:
select descendants(200);

\echo Descendants of 300 is 300
select descendants(300);

\echo Descendants of 400 is 400, 500
select descendants(400);

\echo Descendants of 500 is 500
select descendants(500);

\echo Descendants of 600 is 600, 500
select descendants(600);

\echo Descendants of 700 is 700
select descendants(700);

\echo Now find the sent descendants of each node.

\echo Should return 500:
select find_sent(100);

\echo Should return 500:
select find_sent(200);

\echo Should return empty:
select find_sent(300);

\echo Should return 500:
select find_sent(400);

\echo Should return 500:
select find_sent(500);

\echo Should return 500:
select find_sent(600);

\echo Should return empty:
select find_sent(700);

\echo Now find nodes with descendants that never got sent.

\echo This should 300 - withered child of 100:
select find_withered(100);
\echo This should return empty:
select find_withered(200);
\echo This should 300 - its withered:
select find_withered(300);
\echo This should return empty:
select find_withered(400);
\echo This should return empty:
select find_withered(500);
\echo This should return empty:
select find_withered(600);
\echo This should return 700 - its withered.
select find_withered(700);

\echo Find all the ancestors of each node, including the original child node.

\echo Ancestors of 100 is 100
select ancestors(100);

\echo Ancestors of 200 are 200, 100
select ancestors(200);

\echo Ancestors of 300 are 300, 100
select ancestors(300);

\echo Ancestors of 400 are 400, 200, 100
select ancestors(400);

\echo Ancestors of 500 are 500, 400, 600, 200, 100
select ancestors(500);

\echo Ancestors of 600 is 600
select ancestors(600);

\echo Ancestors of 700 is 700
select ancestors(700);


