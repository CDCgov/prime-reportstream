/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

-- sets all receivers that have transport type of REDOX to have null transport type in case one gets rehydrated
update setting
set values = jsonb_set(values, '{transport}', 'null', false)
where setting_id in (
	select setting_id
	from setting
	where type = 'RECEIVER'
	and (
		values -> 'transport' ->> 'type' = 'REDOX'
	)
)