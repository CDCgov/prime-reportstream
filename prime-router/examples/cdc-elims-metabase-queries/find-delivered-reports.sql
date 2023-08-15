with recursive children_graph as (
select
	parents.parent_report_id,
	parents.child_report_id
from
	report_lineage parents
join report_file rf on
	rf.report_id = parents.parent_report_id
join "action" a on
	a.action_id = rf.action_id
	where a.action_name = 'receive'
	and rf.schema_topic = 'elr-elims'
	[[and parents.parent_report_id = {{report_id}}::uuid]]
	[[and regexp_match(a.action_params, '\"payloadName\" : \"(.*)\"'))[1] = {{file_name}}]]
union all
select
	children.parent_report_id,
	children.child_report_id
from
	report_lineage children
join children_graph on
	children_graph.child_report_id = children.parent_report_id
)
select
	report_id as "Delivered Report ID",
	report_file.created_at as "Delivered Report Timestamp",
	report_file.external_name as "Delivered Report Name",
	report_file.receiving_org || '.' || report_file.receiving_org as "Report Delivered to"
from
	report_file
join action a on
	a.action_id = report_file .action_id
join children_graph on
	children_graph.child_report_id = report_file.report_id
where
	a.action_name = 'send'
order by report_file.created_at desc;