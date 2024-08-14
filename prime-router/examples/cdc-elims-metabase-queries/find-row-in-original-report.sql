with recursive parent_item_graph as (
select
	children.parent_report_id,
	children.parent_index,
	children.child_report_id,
	children.child_index
from
	item_lineage children
join report_file rf on
	rf.report_id = children .child_report_id
join "action" a on
	a.action_id = rf.action_id
where a.action_name = 'send'
	-- and rf.schema_topic = 'elr-elims'
	[[and rf.report_id = {{report_id}}::uuid]]
	[[and rf.external_name = {{file_name}}]]
	and children.child_index = {{row_number}}
union all
select
	parents.parent_report_id,
	parents.parent_index,
	parents.child_report_id,
	parents.child_index
from
	item_lineage parents
join parent_item_graph on
		parent_item_graph.parent_report_id = parents.child_report_id
	and parent_item_graph.parent_index = parents.child_index
)
select
    report_id as "Original Report ID"
	report_file.created_at as "Report Submitted At",
	body_url as "Report URL",
	(regexp_match(action.action_params,
	'\"payloadName\" : \"(.*)\"'))[1] as "File Name",
	parent_item_graph.parent_index as "Row in original report"
from
	report_file
join action on
	action.action_id = report_file.action_id
join parent_item_graph on
	parent_item_graph.parent_report_id = report_file.report_id
where
	action.action_name = 'receive'
order by report_file.created_at desc;