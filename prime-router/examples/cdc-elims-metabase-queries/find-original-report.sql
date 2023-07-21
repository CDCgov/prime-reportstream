with recursive parent_graph as (
select
	children.parent_report_id ,
	children.child_report_id
from
	report_lineage children
join report_file rf on
	rf.report_id = children .child_report_id
join "action" a on
	a.action_id = rf.action_id
where a.action_name = 'send'
	and rf.schema_topic = 'elr-elims'
	[[and rf.report_id = {{report_id}}::uuid]]
	[[and rf.external_name = {{report_name}}]]
union all
select
	parents.parent_report_id ,
	parents.child_report_id
from
	report_lineage parents
join parent_graph on
	parent_graph.parent_report_id = parents.child_report_id
)
select
	report_id as "Report ID",
	report_file.created_at as "Report Submitted at",
	body_url "Report File Download URL",
	(regexp_match(a.action_params, '\"payloadName\" : \"(.*)\",'))[1] as "File Name"
from
	report_file
join action a on
	a.action_id = report_file .action_id
join parent_graph on
	parent_graph.parent_report_id = report_file.report_id
where
	a.action_name = 'receive'
order by report_file.created_at desc;