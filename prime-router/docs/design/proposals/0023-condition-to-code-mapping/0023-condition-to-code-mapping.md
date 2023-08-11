## Proposal fo Code-toCondition Mapping

## Background

Public Health reporting in the United States is not uniform in nature with each public health agency (PHA) setting their
own criteria for what conditions need to be reported to that jurisdiction. In the context of electronic lab reporting (ELR), the onus is typically on the reporting entity (lab, hospital, clinic etc.) to determine which result messages qualify for each PHA the entity reports to.

Since ReportStream takes on the burden of identifying the appropriate destination for sender’s messages, it will be necessary to implement a feature to ensure only messages for the appropriate conditions/diseases are allowed to route to each connected receiver. This feature will be similar to the COVID-19 LIVD table in that it will be a table of LOINC codes and their associated conditions that is used to reference received LOINC/SNOMED codes and return their associated condition.

## Assumptions
1.) The CSTE ValueSets that make up the Reportable Conditions Trigger Codes (RCTC) will be sufficient to capture >95% of codes sent to ReportStream. If this turns out not to be the case we can modify the design to include additional data sources.

## Criteria

1.) Must be able to map LOINC and SNOMED codes to condition.
2.) Tables must have the ability to be updated without a PR.
3.) Mapping must account for all 160 conditions available in Report Content Knowledge Management System that relate to ELR (https://www.rckms.org/conditions-available-in-rckms/)
4.) Must be able to add ad-hoc mappings as needed for local codes/LDTs or other non-standard codes

## Out of Scope
The below items are deemed not necessary to accomplish the main task of mapping code-to-condition but are instead enhancements to the core functionality that will have their own work effort.

1.) Utility to check sender test compendiums against current mapping table
2.) Utility to automatically updated tables from Value Set Authority Center API (https://www.nlm.nih.gov/vsac/support/usingvsac/vsacfhirapi.html)


## Design


### Condition Mapping Table

The condition mapping table will be made up of CSTE ValueSets and contain the following columns:

| Column Name                   | Description                                   | Example                                                                        |
|-------------------------------|-----------------------------------------------|--------------------------------------------------------------------------------|
| Member OID                    | ValueSet Identifier                           | 2.16.840.1.113762.1.4.1146.239                                                 |
| Name                          | ValueSet Name                                 | Chlamydia trachomatis Infection (Tests for Chlamydia trachomatis Nucleic Acid) |
| Code                          | LOINC or SNOMED coded value                   | 16601-7                                                                        |
| Descriptor                    | LOINC or SNOMED term descriptio               | Chlamydia trachomatis rRNA [Presence] in Urine by Probe                        |
| Code System                   | Indicates whether code is LOINC or SNOMED     | LOINC                                                                          |
| Version                       | LOINC or SNOMED release version               | 2.74                                                                           |
| Status                        | Indicates if code is active or depracated     | Active                                                                         |
| Condition Name                | Name of associated reportable condition       | Chlamydia trachomatis infection (disorder)                                     |
| Condition Code                | SONMED value associated with condition        | 240589008                                                                      | 
| Condition Code System         | System used for condition code                | SNOMEDCT                                                                       |
| Condition Code System Version | SNOMED version associated with condition code | 2023-03                                                                        |