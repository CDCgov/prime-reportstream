with recursive problem_reports as (
	select
	report_id,
	'There were no receivers to send the report to' as "description",
	'' as "details"
	from
		report_file rf
	where
		next_action = 'none'
		and created_at > now() - interval '7 days'
	union all
	select
	(regexp_match(action.action_params ,
	'report&SEND&([a-z0-9\-]*)&'))[1]::uuid as "report_id",
	'Issue attempting to send the report, will be retried' as "description",
	action.action_result as "details"
	from
		action
	where
		action.action_name = 'send_warning'
		and created_at > now() - interval '7 days'
	union all
	select
	(regexp_match(action.action_params ,
	'report&SEND&([a-z0-9\-]*)&'))[1]::uuid as "report_id",
	'Error encountered sending the report, will not be retried' as "description",
	action.action_result as "details"
	from
		action
	where
		action.action_name = 'send_warning'
		and created_at > now() - interval '7 days'
),
report_graph as (
	select
		children.parent_report_id, problem_reports.description, problem_reports.details
	from
		report_lineage children
	join
		problem_reports
	on
		problem_reports.report_id = children.child_report_id
	where children.created_at > now() - interval '7 days'
	and children.child_report_id in (select distinct on(report_id) report_id from problem_reports order by report_id, created_at asc)
	union all
	select
		parents.parent_report_id, report_graph.description, report_graph.details
	from report_lineage parents
	join
		report_graph
	on
		report_graph.parent_report_id = parents.child_report_id

)
select distinct on (rf.report_id)
	rf.report_id as "Report ID",
	report_graph.description as "Issue Description",
	report_graph.details as "Issue Details",
	rf.created_at as "Received Date",
	(regexp_match(action.action_params, '\"payloadName\" : \"(.*)\"'))[1] as "File Name"
from report_graph
join report_file rf
on rf.report_id = report_graph.parent_report_id
join action on rf.action_id = action.action_id
where action.action_name ='receive'
and rf.schema_topic = 'elr-elims'
order by "Report ID", "Received Date" desc;