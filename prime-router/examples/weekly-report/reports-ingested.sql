\echo List of all reports sent to SR  in the last week
select report_id, item_count, sending_org, sending_org_client, RF.created_at,
       RF.schema_name
from report_file RF
join action A on A.action_id = RF.action_id
where 
  A.action_name = 'receive'
  and RF.sending_org is not null
  and RF.created_at > now() - interval '7 days'
order by receiving_org, receiving_org_svc, RF.created_at
;

