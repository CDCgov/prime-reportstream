\echo Find WIP - Work in Progress.
\echo In practice, that means jobs queued for 'batch'
\echo
\echo From the TASK table:
select report_id, next_action, next_action_at,
       schema_name, receiver_name,
       item_count, body_format, created_at, retry_token
from task
where next_action_at > now()
order by next_action_at;
