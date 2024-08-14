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
 * Add new lookup tables and history.
 */

 /*
  * This table tracks the version history.  Each history entry can have multiple rows in the lookup_table_row table.
  */
 CREATE TABLE lookup_table_version
(
  lookup_table_version_id BIGSERIAL PRIMARY KEY,
  table_name character varying(512) NOT NULL,
  table_version INTEGER NOT NULL CHECK (table_version >= 0),
  is_active BOOLEAN NOT NULL DEFAULT FALSE,
  created_by VARCHAR(63) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX ON lookup_table_version(table_name) WHERE is_active = TRUE;
CREATE UNIQUE INDEX ON lookup_table_version(table_name, table_version);

/*
 * This table stores each table row separately.  Table columns are stored in the data column as JSON to support
 * any number of columns.  Each row references a specific table version in lookup_table_version.  Only one table version
 * for a given table name can be active at a time.
 */
CREATE TABLE lookup_table_row
(
  lookup_table_row_id BIGSERIAL PRIMARY KEY,
  lookup_table_version_id BIGSERIAL NOT NULL,
  row_num INTEGER NOT NULL CHECK (row_num > 0),
  data JSONB NOT NULL,
  CONSTRAINT fk_lookup_table_version
      FOREIGN KEY(lookup_table_version_id)
	  REFERENCES lookup_table_version(lookup_table_version_id)

);
CREATE UNIQUE INDEX ON lookup_table_row(lookup_table_version_id, row_num);
