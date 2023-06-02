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
    This is the "Last Mile" failures query.
    See https://github.com/CDCgov/prime-reportstream/issues/9649
 */
DROP FUNCTION IF EXISTS list_send_failures;
CREATE OR REPLACE FUNCTION list_send_failures(days_to_show_param INT)
    RETURNS TABLE
            (
                action_id            BIGINT,
                report_id            UUID,
                receiver             TEXT,
                file_name            TEXT,
                failed_at           TIMESTAMP WITH TIME ZONE,
                action_params        varchar(2048),
                action_result        TEXT,
                body_url             varchar(2048),
                report_file_receiver TEXT
            )
    LANGUAGE plpgsql
AS
$$

BEGIN
    RETURN QUERY
        WITH send_failures as
                 (select a.action_id
                       , split_part(a.action_params, '&', 3)::uuid
                         as "report_id"
                       , a.created_at as "failed_at"
                       , a.action_params
                       , a.action_result
                       , case
                             when strpos(a.action_result, 'unrecoverable exception') > 0 and
                                  strpos(a.action_result, 'host=') > 0
                                 then trim(split_part(
                                     split_part(a.action_result, 'host=', 2), ',', 1))
                             when strpos(a.action_result, 'unrecoverable exception') > 0 and
                                  strpos(a.action_result, 'database:') > 0
                                 then trim(split_part(
                                     split_part(a.action_result, 'database:', 2), 'Event:', 1))
                             when strpos(a.action_result, 'AS2') > 0
                                 then trim(split_part(
                                     split_part(a.action_result, 'orgService =', 2),
                                     ');', 1))
                            when strpos(a.action_result, 'orgService = ') > 0
                                 then trim(split_part(
                                     split_part(a.action_result, 'orgService =', 2),
                                     '),', 1))
                             when strpos(a.action_result, 'gaen') > 0
                                 then 'wa-phd.gaen'
                             when strpos(a.action_result, 'gaen') = 0 and
                                  strpos(a.action_result, 'Send Error report for: ') >
                                  0
                                 then trim(split_part(
                                     split_part(a.action_result, 'Send Error report for: ', 2),
                                     'to', 2))
                             else 'Cannot parse error message'
                         end
                         as "receiver"
                  from action a
                  where a.created_at >= current_date - days_to_show_param
                    and a.action_name = 'send_error')
        select sf.action_id
             , sf.report_id
             , sf.receiver
             , split_part(rf.body_url, '%2F', 3)
            as "file_name"
             , sf.failed_at
             , sf.action_params
             , sf.action_result
             , rf.body_url
             , rf.receiving_org || '.' || rf.receiving_org_svc
            as "report_file_receiver"
        FROM report_file rf
                 join send_failures sf on rf.report_id = sf.report_id
        ORDER BY sf.failed_at desc;
END;
$$;
