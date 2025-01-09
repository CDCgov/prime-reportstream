# Standard Operating Procedure (SOP) for Validating New Reportable Conditions

---

## Purpose
To ensure that data from new reportable conditions sent by new or existing senders is processed correctly and successfully delivered to state, tribal, local, and territorial (STLT) health departments.

---

## Step 1: Verify LOINC Code Mappings

1. **Check Observation Mapping Table**:
    - Ensure all LOINC codes for new conditions are mapped in ReportStream’s observation mapping table.
    - Verify that any new Ask at Order Entry (AOE) questions included in the message are also mapped.

2. **Address Missing Mappings**:
    - If codes are not mapped, refer to the documentation: [Mapping Sender Codes to Conditions](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/onboarding-users/sender-onboarding/mapping-sender-codes-to-condition.md).

---

## Step 2: Validate LOINC Descriptions and Minimum Data

1. **Ensure Accurate Descriptions**:
    - Verify that LOINC descriptions in ReportStream’s local LOINC table match the expected codes and descriptions.

2. **Confirm Minimum Data**:
    - Ensure the minimum required data is present to represent a test.
    - For FHIR messages:
        - **Test Ordered**: Located under `DiagnosticReport.code.coding`.
        - **Test Performed**: Located under `Observation.code.coding`.
    - For HL7 v2.5.1 messages:
        - **Test Ordered**: Located in `OBR-4`.
        - **Test Performed**: Located in `OBX-3`.

---

## Step 3: Minimal Data Requirements

### FHIR Representation

1. **DiagnosticReport**
    - **status**: The diagnostic report status (e.g., `final`, `partial`, `amended`).
    - **code**: Type of report (e.g., LOINC code for "Complete Blood Count").
    - **subject**: Reference to the patient resource.
    - **effectiveDateTime** or **effectivePeriod**: Clinical relevance time.
    - **issued**: Timestamp when the report was issued.
    - **result**: References to associated **Observation** resources.

2. **Observation**
    - **status**: The observation status (e.g., `final`, `preliminary`).
    - **code**: Type of observation (e.g., "Glucose level" as a LOINC code).
    - **subject**: Reference to the patient.
    - **effectiveDateTime** or **effectivePeriod**: Clinical relevance time.
    - **value[x]**: Observation value, such as:
        - **valueQuantity**: Numeric result (e.g., `10 mg/dL`).
        - **valueString**: Textual result (e.g., "Negative").
    - **referenceRange**: Normal range for quantitative results.

3. **Patient**
    - **identifier**: Unique identifier (e.g., MRN or national ID).
    - **name**: Patient’s full name.
    - **gender**: Patient’s gender.
    - **birthDate**: Patient’s date of birth.

4. **Practitioner** (if applicable)
    - **identifier**: Identifier for the practitioner (e.g., license number).
    - **name**: Practitioner’s full name.

5. **Organization** (if applicable)
    - **name**: Reporting organization’s name.
    - **identifier**: Organization’s identifier (e.g., CLIA number).

### HL7 v2.5.1 Representation

1. **Message Header (MSH Segment)**
    - Essential fields:
        - `MSH-1` (Field Separator): `|`
        - `MSH-2` (Encoding Characters): `^~\&`
        - `MSH-9` (Message Type): `ORU^R01`
        - `MSH-12` (Version ID): `2.5.1`

2. **Patient Identification (PID Segment)**
    - Essential fields:
        - `PID-3` (Patient Identifier List): Unique patient ID.
        - `PID-5` (Patient Name): Patient’s full name.
        - `PID-7` (Date of Birth): Patient’s DOB.
        - `PID-8` (Sex): Gender.

3. **Observation Request (OBR Segment)**
    - Essential fields:
        - `OBR-4` (Universal Service Identifier): Type of test ordered.
        - `OBR-16` (Ordering Provider): Name of the requesting provider.

4. **Observation Result (OBX Segment)**
    - Essential fields:
        - `OBX-2` (Value Type): Type of result (e.g., numeric).
        - `OBX-3` (Observation Identifier): Specific test performed.
        - `OBX-5` (Observation Value): Result value.
        - `OBX-6` (Units): Measurement units.
        - `OBX-7` (Reference Range): Normal range.
        - `OBX-11` (Observation Result Status): Status (e.g., `F` for final).

5. **Specimen (SPM Segment)** (if applicable)
    - Essential fields:
        - `SPM-4` (Specimen Type): Type of specimen (e.g., blood).

---

## Step 4: Validate Example Messages

1. **FHIR Example**:
    - Ask the sender to send a sample message and validate a FHIR message for the new condition.
    - Convert the message to HL7 and ensure no data is being lost during translation
2. **HL7 Example**:
    - Ask the sender to send a sample  HL7 v2.5.1 ORU_R01 message for the new condition.
    - Process the message through the UP and ensure no data is being lost during translation from HL7 -> FHIR -> HL7

---

## Step 5: Test End-to-End Workflow

1. Submit sample messages to the staging environment.
2. Confirm data passes validation checks in ReportStream.
3. Verify successful delivery to the intended STLT systems.


