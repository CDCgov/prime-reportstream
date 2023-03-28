/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration
*/

/*
Add an action_log index and replace brin with btree indexes on action and report_file where needed.
*/
CREATE INDEX IF NOT EXISTS idx_action_log_action_log_id_btree
  ON public.action_log USING btree
  (action_log_id)
  INCLUDE(report_id)
  TABLESPACE pg_default
;

CREATE INDEX IF NOT EXISTS idx_action_created_btree
      ON public.action USING btree
      (created_at,sending_org,action_name)
      TABLESPACE pg_default
;

DROP INDEX IF EXISTS public.idx_action_created_and_sender;

CREATE INDEX IF NOT EXISTS idx_report_file_created_at_btree
  ON public.report_file USING btree
  (created_at,sending_org)
    INCLUDE(item_count)
  TABLESPACE pg_default
;

DROP INDEX IF EXISTS public.idx_report_file_created_and_sender;
