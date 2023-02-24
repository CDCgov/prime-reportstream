# Introduction

This document shall lay bare the functional requirements requested by the various stakeholders of ReportStream for 
searching and filtering data in ReportStream. Requirements shall be tied to use-cases extracted from current and 
previous designs and APIs.

## Glossary

**item** - a CSV row, HL7 V2 message, HL7 V4/FHIR bundle, or another "data type" that is sent to report stream.

**report** - a collection of items. Items are often grouped and sent to receivers via reports instead of being sent 
individually. A report can contain 0, 1, or many items.

**RS** - ReportStream

**data** - the actual data ReportStream receives that potentially contains PII

**metadata** - when referencing items or reports: a deidentified attribute of the item or report

**search** - Retrieving results based on some set of criteria

**filtering** - Removing results from a set based on some criteria

# ReportStream Data Search and Retrieval - Use Cases

> **Note:** If there is no example listed under a use case, that means no existing functionality or design document states this use case. It means the use case was "made up" by some designer or engineer on ReportStream.

## 1. Data Retrieval/Search

**1.1** As a RS sender, I want visibility into the routing of the data I send.
- I want a list of reports I sent out sorted by date.
- I want a list of reports I sent out grouped by date.
- I want a list of reports I sent out sorted by receiver.
- I want a list of reports I sent out grouped by receiver.
- I want a list of delivered reports.
- I want a list of undelivered reports.
- I want a list of reports that have warnings.
- I want a list of reports that have errors.
- I want to see the warning and error messages for a given report.
- I want to see what data was sent to whom and when it was sent.
- I want a list of all receivers that received my data.
- I want a list of items that were filtered out for a delivered report.

**1.2** As a RS sender, I want visibility into the data I am sending.
- I want to see the metadata for a given report.
    - Ex: See [Submissions](https://staging.reportstream.cdc.gov/submissions) tool on website.

**1.3** As a RS receiver, I want visibility into the data I am receiving. See 
[STLT Dashboard Design](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=995%3A13227&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
- I want a list of all reports sent to me. See
[All available reports](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=995%3A13703&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
- I want to filter/group the reports sent to me by the data they contain. See 
[All available reports](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=995%3A13703&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
    - I want to sort reports sent to me by the date they were sent.
    - I want to sort reports sent to me by the date they expire.
    - I want to sort reports sent to me by how many items they contain.
- I want to gain insight into the data I receive by querying the metadata associated with items. See 
[All facilities & providers](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=995%3A13474&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
    - I want a list of all performing facilities sending data to me.
    - I want a list of all ordering providers sending data to me.
    - I want a list of all submitters sending data to me.
    - I want to know the last time a particular performing facility sent data to me.
    - I want to know the last time a particular ordering provider sent data to me.
    - I want to know the last time a particular submitter sent data to me.
- Given some report item data, I want to find other data associated with the item. See 
[Carroll Schultz](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=1081%3A15935&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
    - I want to see when an ordering provider first reported to us, the receiver.
    - I want the average number of tests per report sent to us for all reports including a particular ordering provider.
    - I want the total number of items associated with a particular ordering provider.
    - I want the contact information for a particular ordering provider.
    - I want the CLIA associated with a particular ordering provider.

**1.4** As a member of the engagement team (RS Admin), I want visibility into the data that flows through RS so I can 
better troubleshoot issues. See **Message Tracker** hidden feature on RS website. See 
[Engagement Engineer Document](https://docs.google.com/document/d/18Sk0NxBdn4K_tuMwBbhBdvfDtPjJ3wnEklg6i7taoAE/edit).
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
- I want to get all actions for a sender within a particular time frame. Currently this query times out consistently.

# Design

TODO