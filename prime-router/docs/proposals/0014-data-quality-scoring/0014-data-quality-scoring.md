# Data Quality Scoring

## Introduction
The base goal of an ELR message is to transfer information describing a testing event. 
The impact of that information depends entirely on the data quality. In this context data 
quality represents how closely aligned the content of the message is to what is expected 
by public health jurisdictions for test reporting requirements. While those expectations 
can vary between jurisdictions, there are commonalities across jurisdictions to be 
examined.

## Use Cases
1. Measure quality of incoming data from senders to compare senders and monitor quality overtime
2. Measure quality of outgoing data then compare incoming quality to outgoing quality to gauge
the value-added by ReportStream data formatting and cleansing features

## Analysis
In public health, data quality can be delineated by methodology: on-site assessment and
remote review. This proposal only aims to remote review through automated means using
the ReportStream application. The quality scoring feature will be built into ReportStream
to review quality of data reported from all sources.

This document describes the areas that will be used to gauge data quality and determine a Quality 
Score. For data quality purposes, message fields can be grouped into three categories: 
Essential, Required, Optional. Additionally, reporting latency and usefulness are important ELR metrics.

### Essential Fields
| Description       | COVID-19 Schema Name | HL7 Field Name | HL7 Identifier |
| ----------------- | -------------------- | -------------- | -------------- |
| 
