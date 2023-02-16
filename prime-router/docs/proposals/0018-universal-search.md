# Introduction

This document shall lay bare the functional requirements requested by the various stakeholders of ReportStream for 
searching and filtering data in ReportStream. Requirements shall be tied to use-cases extracted from current and 
previous designs and APIs.

## Glossary

**item** - a CSV row, HL7 V2 message, HL7 V4/FHIR bundle, or another "data type" that is sent to report stream.

**report** - a collection of items. Items are often grouped and sent to receivers via reports instead of being sent 
individually. A report can contain 0, 1, or many items.

**RS** - ReportStream

**metadata** - when referencing items or reports: an attribute of the item or report


# ReportStream Data Search and Retrieval - Use Cases

## 1. Data Retrieval/Search

**1.1** Sender Dashboard/Submissions: As a RS sender, I want visibility into where the data I am sending is going. 
- **1.1.1** I want to see what data is being sent to whom and when it was sent.
	- Ex: I want a list of reports and how many items each contains. See "Submissions" tool on website.
- **1.1.2** I want a list of all receivers for a particular sender
- **1.1.3** I want a list of all senders for a particular receiver

**1.2** STLT Data Dashboard/Daily Data: As a receiver of data, I want to know what data I am receiving and what it 
    contains.[Reference Document](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=995%3A13227&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
- **1.2.1** I want to filter and group items by their metadata. In other words, I want to extract information/metadata based on all the data that goes through the system.
	- Ex: In the case of COVID data, [For a particular receiver, I want a list of all performing facilities, ordering providers, and submitters](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=995%3A13474&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
- **1.2.2** I want a list of all reports sent to me and the ability to filter/group them by their metadata. [Reference Document](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=995%3A13703&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
	- Ex. In the case of COVID data, I want to sort all reports sent to me by the date they were sent, when they expire,
        and how many results they contain.
	- Ex: In the case of COVID data, [I want to know what the most recent report date is for a particular ORDERING PROVIDER.](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=995%3A13474&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
        `ORDERING PROVIDER` is an attribute of a COVID message.
- **1.2.3** Given a single piece of metadata of an item, I want to find any metadata logically associated with it. [Reference Document](https://www.figma.com/proto/6mwI5ac6rprACKDzDo4Ady/ReportStream-Workspace-%7C-2023?node-id=1081%3A15935&scaling=min-zoom&page-id=496%3A6448&starting-point-node-id=995%3A13227&show-proto-sidebar=1).
	- Ex: In the case of COVID data, I want the number of items, and possibly their metadata, that have the 
        `ORDERING PROVIDER` set to "Carroll Schultz". In the linked example, see the "total tests (all time)" attribute.
	- Ex: In the case of COVID data, I want all the reports, and their metadata, where one or more of their items 
        contains "Carroll Schultz" as the `ORDERING PROVIDER`.

**1.3** Message Tracker: As a member of the engagement team (RS Admin), I want to find specific items that flow through 
    RS and their associated metadata.
- **1.3.1** I want to be able to locate individual items by wildcard search of their individual identifier that may not 
    necessarily be unique, like messageID for COVID messages.
- **1.3.2** I want to grab reports or items based on a date range and view their associated metadata.
- **1.3.3** I want to grab reports or items based on specific metadata they contain.
	- Ex: In the case of COVID data, I want to select messages based on their `created_at` attribute or their 
        `testing_lab_name`.
- **1.3.4** Given an item ID, I want all the data associated with the item and the report(s) it belongs to.
	- Ex: In the case of COVID data, I want all the receivers that the message was sent to and their 
        metadata (Date/Time Submitted, Incoming Report Id, Incoming File URL, Receiver Service). See the message tracker page for more detail.

**1.4** As a member of the engagement team (RS Admin), I want to see failed actions and warnings in a specific 
    time range. Currently this is done with a LIKE query on the action result column. 
    [Reference Document](https://docs.google.com/document/d/18Sk0NxBdn4K_tuMwBbhBdvfDtPjJ3wnEklg6i7taoAE/edit).
- **1.4.1** I want to get all actions for a sender within a particular time frame. Currently this query times out 
    consistently.
- **1.4.2** When searching for an item, I want to be able to get data from the associated report.
- **1.4.3** When searching for a report, I want to be able to get data for all associated items of the report.


# ReportStream Data Search and Retrieval - Functional Requirements

## 1. Data Retrieval/Search

**1.1** RS shall make available the retrieval of data related to the routing history of specific items, reports, and actions.
- **1.1.1** RS shall support time-based queries and filtering related to routing history of all items, reports, and actions.

**1.2** RS shall make available the retrieval of metadata belonging to the items and reports that 
    flow through the pipeline.
- **1.2.1** RS shall support full text search/filtering capabilities in regard to items and reports and their 
    associated metadata.
- **1.2.2** RS shall support time-based queries/filters for items, reports, and actions.

**1.3** RS shall easily, and with minimal additional effort, support new data/query requests from any stakeholder.

**1.4** To the fullest extent possible, RS shall enable the client to easily create their own queries/dashboards 
    using the RS API without any help from RS staff.

**1.5** RS shall support the search and filtering of any new "data type" with as minimal additional 
    engineering effort as possible.

**1.6** RS shall be performant in all the possible queries it supports, including text and datetime search. 
- **1.6.1** Report-type queries shall return in less than 10 seconds.
- **1.6.2** Most queries should be able to support a 0-4 second page load time.


## 2. Data Security and Privacy

**2.1** All ReportStream data and search capabilities shall abide by privacy rules and guidelines set by 
    [HHS](https://www.hhs.gov/hipaa/for-professionals/privacy/special-topics/de-identification/index.html).

**2.2** All ReportStream data and search capabilities shall abide by the PRIME
    [Security Assessment Report](https://cdc.sharepoint.com/:w:/r/teams/OCIO-CSPO-PB/srat/_layouts/15/Doc.aspx?sourcedoc=%7B02882F63-DCE4-4FFB-AABA-260F43C0F3A9%7D&file=PRIME_SAR_04142022.docx&action=default&mobileredirect=true) (requires CDC access). If needed, an investigation into ammending the agreements made in the report can be undertaken.

# Design

TODO