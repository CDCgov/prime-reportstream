# HL7v2 to FHIR Inventory

Generally, all the mappings follow
the [inventory](https://docs.google.com/spreadsheets/d/1PaFYPSSq4oplTvw_4OgOn6h2Bs_CMvCAU9CqC4tPBgk/edit#gid=484860251)
specified here. This mapping is comprehensive for mapping from HL7 to FHIR, but it is not a lossless process and the
inventory does not specify how to map from FHIR to HL7.

## Translating from FHIR to HL7

In many cases, the FHIR bundle created by strictly following the inventory does not have enough information to
deterministically
create an HL7 message. A few examples on when this can occur:

- the inventory does not map a field
  -
  see [MSH.2](https://docs.google.com/spreadsheets/d/13pgda5xl-PwCgB9j0axyymwwv7RJVcrIzY8Ah1y1Y1M/edit#gid=0&range=J4)
- the inventory does not map a field if it does not meet certain conditions
  -
  see [MSH.5.1](https://docs.google.com/spreadsheets/d/18o2QLSHQPkRr1S0vax7G4tuuXQnhE9wJl0n1kjupS7U/edit#gid=0&range=G3)
- the inventory inserts different HL7 components into the same array
    - see [XAD](https://docs.google.com/spreadsheets/d/1hSTEur557TIKPEKZRoprVw-uNpw12JZtri-iQsc4uQ0/edit#gid=0&range=J4)
- the inventory specifies preferring one HL7 field over the other
  -
  see [MSH.6](https://docs.google.com/spreadsheets/d/13pgda5xl-PwCgB9j0axyymwwv7RJVcrIzY8Ah1y1Y1M/edit#gid=0&range=G11)

These cases are all handled by adding custom extensions that can then be read when converting to HL7. These extensions
either store
HL7 data not translated to FHIR or information on where a particular piece of a FHIR bundle should be mapped to in the
HL7 message.

## Differences from the inventory

The v2-FHIR inventory is more of a rough target then an official spec and does contain inconsistencies, contradictions,
errors and decisions
that ReportStream does not think match the reality of the data the application receives. The following captures where
the ReportStream
implementation differs from what is in the spreadsheets.

### MSH/SFT -> MessageHeader

- MSH.24 vs MSH.3: The inventory prefers MSH.24 over MSH.3, but ReportStream prefers MSH.3 as from experience it has
  better information
- MSH.25 vs MSH.5: The inventory prefers MSH.25 over MSH.5, but the ReportStream prefers MSH.5 as from experience it has
  better information
- MSH.23 vs MSH.6: The inventory prefers MSH.23 over MSH.6, but the ReportStream prefers MSH.5 as from experience it has
  better information
- The inventory specifies two different, contradicting mappings for MessageHeader.source and the team opted to
  use [HD[MessageHeader.source.endpoint]](https://docs.google.com/spreadsheets/d/18o2QLSHQPkRr1S0vax7G4tuuXQnhE9wJl0n1kjupS7U/edit#gid=0)
  as it is more specific
- The inventory specifies two different, contradicting mappings for MessageHeader.destination and the team opted to
  use [HD[MessageHeader.destination.endpoint]](https://docs.google.com/spreadsheets/d/1T99UdnCSjoGpbamAvfVEZMDN5wKRtc0gUlWZ0ufRd6c/edit#gid=0)
  as it is more specific
- The inventory specifies that MessageHeader.destination should have a reference to a device, but there is no mapping so
  that is not implemented

### OBR/ORC -> ServiceRequest

- There is a discrepancy on where to pull identifiers from, for ORC/OBR 2,3 the mapping contradictorily states that both
  should be preferred over the other.
  The implementation opts to operate with the same logic for mapping to DiagnosticReport and to prefer ORC when
  available
- The inventory believes that ORC.4 will sometimes be an EIP rather than EI, but the NIST spec and the HAPI structures
  all indicate that it is EI, the implementation will map it ORC.4 twice with different constants
- ORC.7 does have a mapping, but is withdrawn/deprecated and is not mapped
- The inventory doesn't specify how resolve ORC.1 and OBR.11 which both target `intent`, the implementation favors the
  mapping in OBR since it is more specific
- The inventory specifies that OBR.13 should be added as an extension to supportingInfo which is an array, the
  implementation simply adds it as an extension on ServiceRequest
- The inventory mentions OBR.29, ORC.8 and ORC.31 mention that they should be mapped onto a `basedOn` value which is not
  defined in the mapping, the implementation maps them to extensions
- The inventory specifies to prefer OBR.53 over ORC.33 as an identifier which does not align with any of the other
  identifiers, the implementations prefer ORC in all casses

### PID -> Patient

- PID.2 is deprecated in the HL7v2.7 and NIST HL7v2.5.1 specs. Further, the HAPI v2.7 model has set both fields to
  NULLDT. Thus, this field is not being mapped.
- PID.4 is deprecated in the HL7v2.7 and NIST HL7v2.5.1 specs. Further, the HAPI v2.7 model has set both fields to
  NULLDT. Thus, this field is not being mapped.
- PID.6: The inventory maps the value to a `valueString` extention but `valueHumanName` is available and fully captures
  the values, so it is being used over `valueString`
- PID.12 is deprecated in the HL7v2.7 and NIST HL7v2.5.1 specs. Further, the HAPI v2.7 model has set both
  fields to NULLDT. Thus, this field is not being mapped.
- PID.19 and PID.20 are deprecated in the HL7v2.7 and NIST HL7v2.5.1 specs. Further, the HAPI v2.7 model has set both
  fields to NULLDT. Thus, this field is not being mapped.
- PID.21: The inventory does not include a FHIR field on patient that this should be mapped to. We have decided to map
  to Patient.link which includes a reference to RelatedPerson

### PD1 -> Patient

- PD1.4 is deprecated in the HL7v2.7 and NIST HL7v2.5.1 specs. Further, the HAPI v2.7 model has set both fields to
  NULLDT. Thus, this field is not being mapped.

### PV1 -> Patient

- Mapping inventory only defines PV1.16 mapping and only when the value is `VIP`. PV1.16 is deprecated in NIST HL7v2.5.1
  spec and is more thoroughly mapped in PV1 -> Encounter. This field does not need to be mapped to Patient.

### NK1 -> Patient

- Mapping comments
  in [ORU_R01](https://docs.google.com/spreadsheets/d/1gHK6_PFyr7PXns7wLDs0LSLsbjm0x-4bWUu3crXMKMI/edit#gid=0)
  indicate that NK1 can be mapped to Patient or Related Person. We have decided Related Person is more appropriate.
### PV1/PV2 -> Encounter

- The inventory says PV1.16 should go to a property on an Encounter, but that property exists on
  Encounter.hospitalization which is where the implementation sets it
- The NIST spec states that PV1.2 is of type `IS` but in both the inventory and the HAPI structures it is a CWE which is
  how it is mapped
- The inventory does not have a record for the FC datatype and the implementation does not map PV1.20
- PV1.52 should be mapped as it's in NIST and has a mapping in the inventory but, the HAPI 2.7 structures have it as
  NULLDT so this implementation does not map it
