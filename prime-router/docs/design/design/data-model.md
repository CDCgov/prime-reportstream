TODO: Document our use of Postgres and how we use it to store "metadata"

# ReportStream Data Model and Metadata

## Overview

Data is captured and stored in a relational database (Postgres) as [Reports](../../universal-pipeline/README.md#report-and-item) flow through ReportStream. This data can generally be referred to as "metadata". The captured metadata aims to track and explain the ReportStream-related history of a Report so questions like the following can be answered (non-exhaustive list):

1. Did a particular Report get delivered? and to whom?
2. What Reports were not delivered anywhere? Were they supposed to be delivered somewhere?
3. When there is an issue, what were the error or warnings associated with the processing of a particular Report?
4. What data did a Report contain?

> The data that is contained within a Report or Item (like ordering provider) is often referred to as "metadata", and should not be confused with the more encompassing "ReportStream metadata." This document shall discuss both, and will use the terms "Report/Item metadata" and "ReportStream (RS) metadata" to distinguish between the two concepts

This document shall cover the data and its organization that makes answering the questions outlined above possible, hereafter referred to as the ReportStream data model.

## ReportStream Postgres Data Model

All ReportStream metadata is stored in ReportStream's internal Postgres database. The data is split up into various tables and relationships between the tables allow for some complex queries. The following sections are based on the real world data ReportStream models, and each section may discuss one or more database tables. This is opposed to having a section for each database table, as oftentimes multiple tables work together to model some real world data.

### Actions

#### Associated Tables

- action
- action_log

### Report and Item Lineage

#### Associated Tables

- report_file
- report_lineage
- item_lineage

### Lookup Tables

#### Associated Tables

- lookup_table_row
- lookup_table_version

### Settings

#### Associated Tables

- setting

#### Organization Settings

##### API KEYS

### ReportStream Backend

This section shall discuss the rest of the DB tables that power backend or non-user facing functionality of ReportStream.