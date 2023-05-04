# Sample queries for analysis

These are pulled from the initial search requirements and the purpose of this document was to evaluate the
feasibility of the solution that includes

- storing the terminal report ids in a mapping table
- storing FHIR resources in elastic search indices.

It is mostly a scratch pad created as parted of the speccing process.

**1.1** As a RS sender, I want visibility into the routing of the data I send.
```sql
with delivered_reports_for_senders as (select
 delivered.*
from report_file delivered
join terminal_report_ids on terminal_report_id = delivered.report_id
join report_file submitted on submitted.report_id = terminal_report_ids.original_report_id
where delivered.transport_result is not null
and submitted.sending_org = %sending_org%
```

- I want a list of reports I sent out sorted by date.
```sql
select * from delivered_reports_for_senders
order by created_at desc;
```
- I want a list of reports I sent out grouped by date.
```sql
select * from delivered_reports_for_senders
group by created_at;
```
- I want a list of reports I sent out sorted by receiver.
```sql
select * from delivered_reports_for_senders
order by receiving_org;
```
- I want a list of reports I sent out grouped by receiver.
```sql
select * from delivered_reports_for_senders
group by receiving_org;
```
- I want a list of delivered reports.

select * from delivered_reports_for_senders;

- I want a list of undelivered reports.
[Out of scope](./0020-universal-metadata-search.md#searches-to-be-split-into-other-tickets)
- I want a list of reports that have warnings.
```sql
select 
from action_log
join report_file
where type='warning'
where sending_org=%sender% 
```
- I want a list of reports that have errors.
```sql
select 
from action_log
join report_file
where type='error'
where sending_org=%sender% 
```
- I want to see the warning and error messages for a given report.
Similar to finding reports with errors or warnings
- I want to see what data was sent to whom and when it was sent.
Similar to fetching a list of delivere reports
- I want a list of all receivers that received my data.
```sql
select distinct receiving_org from delivered_reports_for_senders;
```
- I want a list of items that were filtered out for a delivered report.
[Out of scope](./0020-universal-metadata-search.md#searches-to-be-split-into-other-tickets)

**1.2** As a RS sender, I want visibility into the data I am sending.
- I want to see the metadata for a given report.
    - Ex: See [Submissions](https://staging.reportstream.cdc.gov/submissions) tool on website.
```sql
select * from metadata
where report_id = %report_id%;
```

**1.3** As a RS receiver, I want visibility into the data I am receiving. See
[STLT Dashboard Design](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=995%3A13227&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
```sql
with delivered_reports_for_receiver as (select
 delivered.*
from report_file delivered
join terminal_report_ids on terminal_report_id = delivered.report_id
join report_file submitted on submitted.report_id = terminal_report_ids.original_report_id
where delivered.transport_result is not null
and submitted.sending_org = %receiving_org%)
```


- I want a list of all reports sent to me. See
  [All available reports](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=995%3A13703&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
```sql
select * from delivered_reports_for_receiver;
```
- I want to filter/group the reports sent to me by the data they contain. See
  [All available reports](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=995%3A13703&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
    - I want to sort reports sent to me by the date they were sent.
  
    ```sql
    select * from delivered_reports_for_receiver
    order by created_at
    ```

    - I want to sort reports sent to me by the date they expire.

  [Out of scope](./0020-universal-metadata-search.md#searches-to-be-split-into-other-tickets)
    - I want to sort reports sent to me by how many items they contain.
    ```sql
    select * from delivered_reports_for_receiver
    order by item_count;
    ```
  - I want to gain insight into the data I receive by querying the metadata associated with items. See
    [All facilities & providers](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=995%3A13474&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
      - I want a list of all performing facilities sending data to me.
   
      ???
      - I want a list of all ordering providers sending data to me.
  
      ???
      - I want a list of all submitters sending data to me.
      ```sql
      select
      distinct submitted.sending_org
      from report_file delivered
      join terminal_report_ids on terminal_report_id = delivered.report_id
      join report_file submitted on submitted.report_id = terminal_report_ids.original_report_id
      where delivered.transport_result is not null
      and submitted.sending_org = %receiving_org%
      ```
      - I want to know the last time a particular performing facility sent data to me.
          - Search the facility index and get all the metadata ids
        ```sql
                select min(metadata.created_at) from metadata
                where id in %metadata_ids%
                join terminal_report_ids on(
                    terminal_report_ids.origin_report_id = metadata.report_id
                    and terminal_report_ids.origin_report_index = metadata.report_index
                )
                join report_file on(
                    terminal_report_ids.terminal_report_id = report_file.report_id
                )
            where report_file.receiving_org = %receiving_org%
            ```
      

      - I want to know the last time a particular ordering provider sent data to me.

      Mostly the same as facility
  
      - I want to know the last time a particular submitter sent data to me.
      ```sql
      select
      max(submitted.created_at)
      from report_file delivered
      join terminal_report_ids on terminal_report_id = delivered.report_id
      join report_file submitted on submitted.report_id = terminal_report_ids.original_report_id
      where delivered.transport_result is not null
      and submitted.sending_org = %receiving_org%
      ```
- Given some report item data, I want to find other data associated with the item. See
  [Carroll Schultz](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=1081%3A15935&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
    - I want to see when an ordering provider first reported to us, the receiver.
        - Search the ordering provider index and get all the metadata ids
        ```sql
        select min(metadata.created_at) from metadata
        where id in %metadata_ids%
        join terminal_report_ids on(
            terminal_report_ids.origin_report_id = metadata.report_id
            and terminal_report_ids.origin_report_index = metadata.report_index
        )
        join report_file on(
            terminal_report_ids.terminal_report_id = report_file.report_id
        )
       where report_file.receiving_org = %receiving_org%
        ```
    - I want the average number of tests per report sent to us for all reports including a particular ordering provider.
      - Similar to above queries for finding reports with an ordering provider
    - I want the total number of items associated with a particular ordering provider.
      - Search the ordering provider index and get all the metadata ids
      ```sql
      select count(1) from metadata
      where id in %metadata_ids%
      join terminal_report_ids on(
          terminal_report_ids.origin_report_id = metadata.report_id
          and terminal_report_ids.origin_report_index = metadata.report_index
      )
      join report_file on(
          terminal_report_ids.terminal_report_id = report_file.report_id
      )
      where report_file.receiving_org = %receiving_org%
        ```
    - I want the contact information for a particular ordering provider.
        - Find the ordering provider in the index and return the contact info
    - I want the CLIA associated with a particular ordering provider.
      - Find the ordering provider in the index and return the CLIA

**1.4** As a member of the engagement team (RS Admin), I want visibility into the data that flows through RS so I can
better troubleshoot issues. See **Message Tracker** hidden feature on RS website. See
[Engagement Engineer Document](https://docs.google.com/document/d/18Sk0NxBdn4K_tuMwBbhBdvfDtPjJ3wnEklg6i7taoAE/edit).
All these are searchable without needing to go elastic search and can use the terminal report id to link back to the
original item
- I want to find the metadata associated with a particular report item given a non-unique piece of metadata, like a
  messageID in the case of a COVID message.
- I want to search report items based on the date they were created or the testing lab they are associated with.
- I want to search reports based on a date range.
- I want to search items of reports based on a date range.
- I want to view the metadata associated with a particular report.
- I want to view the metadata associated with a particular item of a report.
- Given a unique item identifier, I want all the metadata associated with the item, including the report(s) it belongs to. See **Message Tracker**.

**1.5** As a member of the engagement team (RS Admin), I want to see failed actions and warnings in a specific
time range. [Reference Document](https://docs.google.com/document/d/18Sk0NxBdn4K_tuMwBbhBdvfDtPjJ3wnEklg6i7taoAE/edit).
- I want to get all actions for a sender within a particular time frame. Currently this query times out consistently.x```