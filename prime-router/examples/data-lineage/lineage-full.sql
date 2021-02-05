\echo >>>>> Find the entire lineage, starting from a simple-report

select RF.report_id, A.action_name as creating_action,
       RF.body_format, RF.sending_org, RF.receiving_org, left(RF.transport_result, 20) as send_result, RF.item_count
from report_file as RF
join action as A ON A.action_id = RF.action_id
where RF.report_id in (select report_descendants(:'param_id'))
order by A.action_id, RF.sending_org;
