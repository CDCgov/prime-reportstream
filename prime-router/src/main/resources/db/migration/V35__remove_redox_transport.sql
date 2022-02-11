/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

-- deactivate all receivers who are set to use redox, nullify redox settings
update setting
set is_active = false,
is_deleted = true
where type = 'RECEIVER'
	and (
	values -> 'translation' ->> 'format' = 'REDOX'
	OR values -> 'transport' ->> 'type' = 'REDOX'
);

update setting
set values = jsonb_set(values, '{transport}', 'null', false)
where setting_id in (
	select setting_id
	from setting
	where type = 'RECEIVER'
	and (
	values -> 'translation' ->> 'format' = 'REDOX'
	OR values -> 'transport' ->> 'type' = 'REDOX'
	)
);

update setting
set values = jsonb_set(values, '{translation}', 'null', false)
where setting_id in (
	select setting_id
	from setting
	where type = 'RECEIVER'
	and (
	values -> 'translation' ->> 'format' = 'REDOX'
	OR values -> 'transport' ->> 'type' = 'REDOX'
	)
);