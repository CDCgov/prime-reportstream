# HL7v2 to FHIR Inventory

Generally, all the mappings follow
the [inventory](https://docs.google.com/spreadsheets/d/1PaFYPSSq4oplTvw_4OgOn6h2Bs_CMvCAU9CqC4tPBgk/edit#gid=484860251)
specified here. This mapping is comprehensive for mapping from HL7 to FHIR, but it is not a lossless process and the
inventory does not specify how to map from FHIR to HL7.

## Translating from FHIR to HL7

In many cases, the FHIR bundle created by strictly following the inventory does not have enough information to
deterministically
create an HL7 message. A few examples on when this can occur:

-   the inventory does not map a field
    -   see [MSH.2](https://docs.google.com/spreadsheets/d/13pgda5xl-PwCgB9j0axyymwwv7RJVcrIzY8Ah1y1Y1M/edit#gid=0&range=J4)
-   the inventory does not map a field if it does not meet certain conditions
    -   see [MSH.5.1](https://docs.google.com/spreadsheets/d/18o2QLSHQPkRr1S0vax7G4tuuXQnhE9wJl0n1kjupS7U/edit#gid=0&range=G3)
-   the inventory inserts different HL7 components into the same array
    -   see [XAD](https://docs.google.com/spreadsheets/d/1hSTEur557TIKPEKZRoprVw-uNpw12JZtri-iQsc4uQ0/edit#gid=0&range=J4)
-   the inventory specifies preferring one HL7 field over the other
    -   see [MSH.6](https://docs.google.com/spreadsheets/d/13pgda5xl-PwCgB9j0axyymwwv7RJVcrIzY8Ah1y1Y1M/edit#gid=0&range=G11)

These cases are all handled by adding custom extensions that can then be read when converting to HL7. These extensions either store
HL7 data not translated to FHIR or information on where a particular piece of a FHIR bundle should be mapped to in the HL7 message.

## Differences from the inventory

The v2-FHIR inventory is more of a rough target then an official spec and does contain inconsistencies, contradictions, errors and decisions
that the ReportStream does not think match the reality of the data the application receives. The following captures where the ReportStream
implementation differs from what is in the spreadsheets.

### MSH/SFT -> MessageHeader

-   MSH.24 vs MSH.3: The inventory prefers MSH.24 over MSH.3, but the ReportStream prefers MSH.3 as from experience it has better information
-   MSH.25 vs MSH.5: The inventory prefers MSH.25 over MSH.5, but the ReportStream prefers MSH.5 as from experience it has better information
-   MSH.23 vs MSH.6: The inventory prefers MSH.23 over MSH.6, but the ReportStream prefers MSH.5 as from experience it has better information
-   The inventory specifies two different, contradicting mappings for MessageHeader.source and the team opted to use [HD[MessageHeader.source.endpoint]](https://docs.google.com/spreadsheets/d/18o2QLSHQPkRr1S0vax7G4tuuXQnhE9wJl0n1kjupS7U/edit#gid=0) as it is more specific
-   The inventory specifies two different, contradicting mappings for MessageHeader.destination and the team opted to use [HD[MessageHeader.destination.endpoint]](https://docs.google.com/spreadsheets/d/1T99UdnCSjoGpbamAvfVEZMDN5wKRtc0gUlWZ0ufRd6c/edit#gid=0) as it is more specific
-   The inventory specifies that MessageHeader.destination should have a reference to a device, but there is no mapping so that is not implemented
