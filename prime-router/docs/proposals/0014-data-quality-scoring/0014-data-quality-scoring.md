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
All fields are useful in some capacity, but some are essential such that when absent the message 
can no longer be used for public health purposes. These fields include:

![EssentialFields](essential_fields.png "Essential Fields")

### Required Fields
Required fields include fields identified by the HL7 2.5.1 R2 standard for public health ELR as 
required as well as the CARES Act Section 18115. While essential fields fall into this category 
as well, this document separates those fields above due to their unique importance. The required, 
but non-essential, fields include:

![RequiredFields](required_fields.png "Required Fields")

### Optional Fields
Optional fields include fields identified by the HL7 2.5.1 R2 standard for public health ELR as optional.

![OptionalFields](optional_fields.png "Optional Fields")

### Scoring Metrics
#### Completeness
A simple test for on or off. Is there a values present in the expected field or is the field blank.

#### Accuracy
Some required fields are use should only include values present in a reference table (ex. LOINC and SNOMED). 
Accuracy in those fields will test if the values reported are in the expected format. Fields where the accuracy
test can be applied:

- Essential Fields
  - Observation Identifier (Test Type)
    - Valid LOINC Code
    - Valid LOINC Description
  - Observation Value (Test Result) if Observation type is CE or CWE
    - Valid SNOMED Code
    - Valid SNOMED Description

- Required Fields
   - Sending Facility CLIA
     - Valid CLIA
   - Test Result date
     - Valid non-future date
   - Patient race
     - Value in HL70005
   - Patient ethnicity
     - Value in HL70189
   - Patient sex
     - Value in HL70001
   - Patient residence zip code
     - Valid Zip
   - Patient residence county
     - Valid County 
   - Ordering Facility Phone
     - Number of digits is correct
   - Result Status
     - Value in HL70123
   - Performing facility CLIA number
     - Valid CLIA
   - Specimen Source
     - Use appropriate LOINC, SNOMED-CT, or SPM-4 codes, or equivalently detailed alternative codes
   - Date test ordered
     - Valid non-future date
   - Date specimen collected
     - Valid non-future date
   - Date specimen received
     - Valid non-future date
   - Patient age
     - Positive number
    
- Optional Fields
  - Patient DOB
    - Valid non-future date 
  - Patient phone
    -  Correct number of digits
  - Ordering provider phone
    - Correct number of digits
  - Observation Method (Device ID OBX-17)
    - Value in LIVD table
  - Equipment Instance Identifier (OBX-18)
    - Value in LIVD table
  - Date of analysis (Observation Date OBX)
    - Valid non-future date
  - Specimen Collection Method (SPM-7)
    - Value in HL70488
  - Ordering provider NPI (as applicable)
    - Valid NPI number
  - Ordering provider zip
    - Valid zip
  - Performing facility zip code
    - Valid zip code

## Proposal

Being that ReportStream is an ETL tool the data will be transformed by processes in the system and 
thus the quality may differ between input and output. I propose first that we measure quality in the 
covid-19 schema. To do so, a field should be added to the schema to hold the score(quality_score).
A new mapper would review the values in the fields identified above then return a score. The scoring 
system will be based on points with a threshold value. The threshold value will have a standard default, 
or it can be set for each organization. Each group listed above (Essential, Required, Optional) are 
assigned a point value. To start we will analyze the data using the completeness and accuracy metrics. 
Starting with a total value of zero, when that field fails a test the point value for its group is 
added to the total value. The sum total value creates the quality score. The quality 
score is then compared to the threshold value. A high score is bad! If the quality score is above or 
equal to the threshold then the message should be considered below quality.
