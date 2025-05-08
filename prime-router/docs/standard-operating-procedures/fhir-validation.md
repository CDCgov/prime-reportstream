# FHIR Validation
ReportStream senders and receivers can either use HL7v2 or FHIR for ELR reports. This document specifies exactly what
data is required for sending FHIR ELR reports to ReportStream and what to expect when receiving FHIR ELR reports.
It specifies which FHIR standards ReportStream conforms to.

# FHIR Messaging
RS conforms to the [FHIR Messaging](https://build.fhir.org/messaging.html) standard. This means that a RS ELR report
consists of a [Bundle](https://build.fhir.org/bundle.html) resource whose type is `message` and the first entry is
a [MessageHeader](https://build.fhir.org/messageheader.html) whose `focus` is a 
[DiagnosticReport](https://hl7.org/fhir/R4/diagnosticreport.html) along with a
[Provenance](https://hl7.org/fhir/provenance.html). Every other entry in the Bundle is referenced by these resources
directly or indirectly.

# FHIR Profiles
Every resource in a RS ELR report must conform to the FHIR profiles specified in this document. We follow the
recommendations listed in the [Federal FHIR Action Plan](https://www.healthit.gov/isp/about-fhir-action-plan).
A FHIR profile specifies what data is required for a given system. This document specifies exactly what FHIR standards
are used by ReportStream. If a resource type has a US Public Health profile, then that profile is used. If not,
then the US Core profile is used. If neither one exists, then the base FHIR R4 profile is used.

### FHIR Profiles required by ReportStream
| FHIR Resource Type     | Implementation Guide                                                                                                      | Canonical URL                                   |
|------------------------|---------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| Bundle (for reporting) | [US Public Health Reporting Bundle](https://hl7.org/fhir/us/ph-library/StructureDefinition-us-ph-reporting-bundle.html)| http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-reporting-bundle |
| Bundle (for content)   | [US Public Health Content Bundle](http://hl7.org/fhir/us/ph-library/StructureDefinition-us-ph-content-bundle.html)| http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-content-bundle |
|Device            | [R4 Device](https://hl7.org/fhir/R4/device.html)| http://hl7.org/fhir/StructureDefinition/Device                                                        |
| DiagnosticReport       | [US Core DiagnosticReport](https://hl7.org/fhir/us/core/StructureDefinition-us-core-diagnosticreport-lab.html)| http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-lab |
| MessageHeader          | [US Public Health MessageHeader](https://www.hl7.org/fhir/us/ph-library/StructureDefinition-us-ph-messageheader.html)| http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-messageheader |
| Observation            | [US Public Health Laboratory Result Observation Profile](https://www.hl7.org/fhir/us/ph-library/StructureDefinition-us-ph-messageheader.html)|http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation|
| Organization           | [US Public Health Organization](https://build.fhir.org/ig/HL7/fhir-us-ph-library/StructureDefinition-us-ph-organization.html)|http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-organization|
| Patient                | [US Public Health Patient](https://build.fhir.org/ig/HL7/fhir-us-ph-library/StructureDefinition-us-ph-patient.html)|http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-patient|
| Practitioner           | [US Core Practitioner](https://hl7.org/fhir/us/core/StructureDefinition-us-core-practitioner.html)|http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitioner|
| PractionerRole         | [US Public Health PractitionerRole](https://hl7.org/fhir/us/core/StructureDefinition-us-core-practitioner.html)|http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-practitionerrole|
| Provenance             | [US Core Provenance](https://hl7.org/fhir/us/core/StructureDefinition-us-core-provenance.html)|http://hl7.org/fhir/us/core/StructureDefinition/us-core-provenance|
| Specimen               | [US Core Specimen](https://hl7.org/fhir/us/core/StructureDefinition-us-core-specimen.html)                                                                                                   |http://hl7.org/fhir/us/core/StructureDefinition/us-core-specimen|
| ServiceRequest         | [US Core ServiceRequest](https://hl7.org/fhir/us/core/StructureDefinition-us-core-servicerequest.html)                                                                                             |http://hl7.org/fhir/us/core/StructureDefinition/us-core-servicerequest|

# Current FHIR Validation Status
Currently, RS doesn't perform FHIR validation. There are known non-conformances issues with SimpleReport inputs as well as internally
stored FHIR documents. In the future we intend to perform FHIR validation when a sender submits a FHIR ELR report and return
an error response if it isn't valid. We also intend to store and send valid FHIR in the future. These efforts have been
put on hold. We also intend to provide a FHIR validation CLI tool. There is currently an implementation of a FHIR validator 
described in the next section.

# Running FHIR validation
There is a class called gov.cdc.prime.router.fhirvalidation.RSFhirValidator that implements FHIR validation.
There is an example of how to use it in gov.cdc.prime.router.fhirvalidation.FhirValidatorTests. Basically
you create a validator, then call validateResource. validateResource can take a FHIR Resource object, or a URL
to a FHIR document.
If you want to validate a file in the resources dir, use validateFhirInResourcesDir. All these functions take
an optional addProfiles parameter that specifies whether or not to add meta.profile elements to RS resources.
Without these resources, it will only validate against FHIR R4.

Example:
`
val validator = RSFhirValidator()
val result = validator.validateFhirInResourcesDir("/fhirsamples/SR-bundle-original.fhir.json")

## Making SimpleReport input FHIR Conformant
The test results below show 

## Samples
There are samples in prime-router/src/test/resources/fhirsamples.
SR-bundle-original.fhir.json is the original SR fhir.
SR-bundle-fixed.fhir.json has been updated to pass FHIR R4
SR-bundle-with-proifles.fhir.json is the original with meta.profiles added

## Utility
There is a utility called addFhirProfiles that will add meta.profile elements to fhir files.

# Unit test
Currently, there is one unit test that runs three validations on some of the samples in
prime-router/src/test/resources/fhirsamples. The first one runs on SR-bundle-original.fhir.json without profiles, 
which means it only validates  against the base FHIR R4 profile. The second test includes profiles, so it validates 
against all the identified profiles from US Public Health Profile Library (USPHPL) and US Core.

The third one runs on SR-bundle-fixed.fhir.json. This sample fixes the errors for base R4. There are no errors
when running without profiles.

I ran the test directly from Intellij. It should be possible to run it from the command line using
```
./gradlew test --tests gov.cdc.prime.router.fhirvalidation.FhirValidatorTests
```
But I got the following error. I need to figure out why.
```
> Task :auth:test FAILED
```
I've proposed a follow-on story to implement a new CLI command for FHIR validation. For now, if you want to validate
a different file, you can create a test modeled on `test FHIR validation SR sample` and call validateAndPrintResults
with your pathname.

## Specifying which profiles to use

The recommended best practice is to use meta.profile elements to specify which profiles to use for validation. 
You input resources will need to have a `meta.profile` element into them. As a model, take a look at 
`prime-router/src/test/resources/fhirsamples/SR-bundle-with-profiles.fhir.json`. I wrote a utility called
`gov.cdc.prime.router.fhirvalidation.RSFhirValidator.addFhirProfiles` that
will create a copy of a fhir file, with the meta.profiles added. The new file tacks a `.json` to the end of the input
file name.

BTW, the convention should be to use .fhir.json as the file type for files containing fhir as json.
FHIR can be represented in XML, but the main reason is that Intellij will know it's a json file and open an
editor that knows json. I use code folding a lot to analyze json.

## Why use meta.profile?

Using using meta.profile in resources is a best practice - especially for nested resources.
Without meta.profile, validators assume the base definition, which might not catch important issues. It's
possible to configure a validator to be preloaded with profiles, but this isn't recommended in general.

### Reasons for using meta.profile
* It's better for interoperability across systems. Receiving systems may depend on meta.profile to understand what
flavor of the resource they're getting.
* Clarity for developers and integrators. meta.profile is an easy, inspectable way to tell what rules this resource was built to follow.
* Makes it easier to troubleshoot, especially when a resource fails validation or isn't accepted by an API.
* Can specify explicit versions e.g. `http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient|6.1.0`

## Follow-on tasks
Add a [MethodDefinition](https://build.fhir.org/messagedefinition.html) definition to RS conformance resources.

# Performance Results
I ran two validations 4/17/25. The first one was only with the FHIR R4 base. It took 2.04 seconds and found 19 errors, 52 warnings, and 18 information notes.
The second one included all identified USPHPL and US Core profiles. It 121ms with the caches warmed up. It found 59 errors, 71 warnings, and 711 information notes.
The detailed results are in a section below.

# Validation Notes
The HAPI FHIR validation configuration is implemented in gov.cdc.prime.router.fhirvalidation.RSFhirValidator.
It uses two packages of configuration resources that are in the test/resources directory. 
(They will be in the main/resources directory):<br />
hl7.fhir.us.core-7.0.0.tgz<br />
usphpl-package.r4.tgz<br />

These were downloaded from the US Core IG and the USPHPL IG. These two zip files contain all the conformance resources
in order to validate US Core and USPHPL.

## Profile Validation Failures

### US Public Health Reporting Bundle
A [US Public Health Reporting Bundle](https://hl7.org/fhir/us/ph-library/StructureDefinition-us-ph-reporting-bundle.html)
is defined to have two entries: a [US Public Health MessageHeader](https://hl7.org/fhir/us/ph-library/StructureDefinition-us-ph-messageheader.html)
and a [US Public Health Content Bundle](http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-content-bundle)

The [US Public Health Content Bundle](http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-content-bundle)
is a collection bundle



# Detailed validation output
```
Validating resource: /fhirsamples/SR-bundle-original.fhir.json addProfiles: false
Done validating resource. Time: 2.213000750s
There are 19 errors, 52 warnings, and 19 information notes

Errors
    Validation_VAL_Profile_Minimum:1
        Provenance.target: minimum required = 1, but only found 0 (from http://hl7.org/fhir/StructureDefinition/Provenance|4.0.1)
    TERMINOLOGY_TX_SYSTEM_WRONG_HTML:1
        The code system reference https://hl7.org/fhir/R4/valueset-gender-identity.html is wrong - the code system reference cannot be to an HTML page. This may be the correct reference: {unable to determine intended url}
    BUNDLE_ENTRY_URL_ABSOLUTE:17
        The fullUrl must be an absolute URL (not 'MessageHeader/16e3e628-2a6d-4d73-a854-b5a7b7c0b634')
        The fullUrl must be an absolute URL (not 'Provenance/bf58460f-6f26-4844-8edb-5ab6f98a0385')
        The fullUrl must be an absolute URL (not 'DiagnosticReport/3268e1d6-cb84-459b-8640-e7930369d34d')
        The fullUrl must be an absolute URL (not 'Patient/10083d1d-dc8b-4ea0-91fa-8744cf0f013b')
        The fullUrl must be an absolute URL (not 'Organization/719ec8ad-cf59-405a-9832-c4065945c130')
        The fullUrl must be an absolute URL (not 'Practitioner/ee29ccf5-631d-4b35-a6d4-30a61c0eb8d9')
        The fullUrl must be an absolute URL (not 'Specimen/baf09fcd-95d1-48d7-9b12-3f52d57d69f4')
        The fullUrl must be an absolute URL (not 'ServiceRequest/66e1f9cb-aa34-48a8-80b7-495b7d0c3a9f')
        The fullUrl must be an absolute URL (not 'Device/989c07bb-de54-4205-b38e-a5b4b08595d7')
        The fullUrl must be an absolute URL (not 'PractitionerRole/e476cc45-7711-4a24-9e5e-c28be0d10b44')
        The fullUrl must be an absolute URL (not 'Organization/07640c5d-87cd-488b-9343-a226c5166539')
        The fullUrl must be an absolute URL (not 'Observation/f08d453c-b80d-4fe3-83cd-c2fc55d9afeb')
        The fullUrl must be an absolute URL (not 'Observation/f9d62f1e-664f-3d44-9af7-d8002aecc319')
        The fullUrl must be an absolute URL (not 'Observation/c890b146-7cd8-3d26-ac8e-ae22b89996ec')
        The fullUrl must be an absolute URL (not 'Observation/6ba7c37f-fb67-32df-91d4-0fdbf7b71956')
        The fullUrl must be an absolute URL (not 'Observation/dfbd2c06-0bb9-3b87-952e-cbeb213f2f43')
        The fullUrl must be an absolute URL (not 'Observation/d750dcec-a11a-3202-b734-b291c798ae20')


Warnings
    Constraint:17
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
    None of th:9
        None of the codings provided are in the value set 'Provenance activity type' (http://hl7.org/fhir/ValueSet/provenance-activity-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://terminology.hl7.org/CodeSystem/v2-0003#R01)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://terminology.hl7.org/CodeSystem/v2-0301#CLIA)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = null#MNI)
        None of the codings provided are in the value set 'Observation Interpretation Codes' (http://hl7.org/fhir/ValueSet/observation-interpretation|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://terminology.hl7.org/CodeSystem/v2-0078#A)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
    Unable to :1
        Unable to expand ValueSet because CodeSystem could not be found: http://loinc.org
    Coding has:2
        Coding has no system. A code with no system has no defined meaning, and it cannot be validated. A system should be provided
        Coding has no system. A code with no system has no defined meaning, and it cannot be validated. A system should be provided
    Best Pract:22
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
    A code wit:1
        A code with no system has no defined meaning, and it cannot be validated. A system should be provided




Validating resource: /fhirsamples/SR-bundle-original.fhir.json addProfiles: true
{"message":"No profile for resource type: PractitionerRole","thread":"main","timestamp":"2025-05-05T14:02:34.390Z","level":"WARN","logger":"gov.cdc.prime.router.fhirvalidation.RSFhirValidator"}
Done validating resource. Time: 157.000417ms
There are 59 errors, 71 warnings, and 711 information notes

Errors
    Validation_VAL_Profile_Minimum:18
        MessageHeader.extension: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-messageheader|1.0.0)
        MessageHeader.reason: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-messageheader|1.0.0)
        Provenance.target: minimum required = 1, but only found 0 (from http://hl7.org/fhir/StructureDefinition/Provenance|4.0.1)
        Provenance.target: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/core/StructureDefinition/us-core-provenance|7.0.0)
        DiagnosticReport.category: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-lab|7.0.0)
        Patient.deceased[x]: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-patient|1.0.0)
        Patient.communication: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-patient|1.0.0)
        Patient.identifier.system: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-patient|1.0.0)
        Organization.active: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-organization|1.0.0)
        Organization.active: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-organization|1.0.0)
        Organization.telecom: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-organization|1.0.0)
        Organization.address: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-organization|1.0.0)
        Observation.category: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation|1.0.0)
        Observation.category: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation|1.0.0)
        Observation.category: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation|1.0.0)
        Observation.category: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation|1.0.0)
        Observation.category: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation|1.0.0)
        Observation.category: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation|1.0.0)
    Validation_VAL_Profile_Minimum_SLICE:8
        Slice 'MessageHeader.extension:messageProcessingCategory': a matching slice is required, but not found (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-messageheader|1.0.0). Note that other slices are allowed in addition to this required slice
        Slice 'DiagnosticReport.category:LaboratorySlice': a matching slice is required, but not found (from http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-lab|7.0.0). Note that other slices are allowed in addition to this required slice
        Slice 'Observation.category:Laboratory': a matching slice is required, but not found (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation|1.0.0). Note that other slices are allowed in addition to this required slice
        Slice 'Observation.category:Laboratory': a matching slice is required, but not found (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation|1.0.0). Note that other slices are allowed in addition to this required slice
        Slice 'Observation.category:Laboratory': a matching slice is required, but not found (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation|1.0.0). Note that other slices are allowed in addition to this required slice
        Slice 'Observation.category:Laboratory': a matching slice is required, but not found (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation|1.0.0). Note that other slices are allowed in addition to this required slice
        Slice 'Observation.category:Laboratory': a matching slice is required, but not found (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation|1.0.0). Note that other slices are allowed in addition to this required slice
        Slice 'Observation.category:Laboratory': a matching slice is required, but not found (from http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-lab-result-observation|1.0.0). Note that other slices are allowed in addition to this required slice
    Reference_REF_CantMatchChoice:12
        Unable to find a match for profile Organization/719ec8ad-cf59-405a-9832-c4065945c130 among choices: http://hl7.org/fhir/us/ph-library/StructureDefinition/us-ph-organization
        Unable to find a match for profile Organization/719ec8ad-cf59-405a-9832-c4065945c130 among choices: http://hl7.org/fhir/StructureDefinition/Device, http://hl7.org/fhir/us/core/StructureDefinition/us-core-organization, http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient, http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitioner, http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitionerrole, http://hl7.org/fhir/us/core/StructureDefinition/us-core-relatedperson
        Unable to find a match for profile Patient/10083d1d-dc8b-4ea0-91fa-8744cf0f013b among choices: http://hl7.org/fhir/StructureDefinition/Device, http://hl7.org/fhir/StructureDefinition/Group, http://hl7.org/fhir/us/core/StructureDefinition/us-core-location, http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient
        Unable to find a match for profile Observation/f08d453c-b80d-4fe3-83cd-c2fc55d9afeb among choices: http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-lab
        Unable to find a match for profile Patient/10083d1d-dc8b-4ea0-91fa-8744cf0f013b among choices: http://hl7.org/fhir/StructureDefinition/Device, http://hl7.org/fhir/StructureDefinition/Group, http://hl7.org/fhir/StructureDefinition/Substance, http://hl7.org/fhir/us/core/StructureDefinition/us-core-location, http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient
        Unable to find a match for profile Patient/10083d1d-dc8b-4ea0-91fa-8744cf0f013b among choices: http://hl7.org/fhir/StructureDefinition/Device, http://hl7.org/fhir/StructureDefinition/Group, http://hl7.org/fhir/us/core/StructureDefinition/us-core-location, http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient
        Unable to find a match for profile Patient/10083d1d-dc8b-4ea0-91fa-8744cf0f013b among choices: http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient
        Unable to find a match for profile Patient/10083d1d-dc8b-4ea0-91fa-8744cf0f013b among choices: http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient
        Unable to find a match for profile Patient/10083d1d-dc8b-4ea0-91fa-8744cf0f013b among choices: http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient
        Unable to find a match for profile Patient/10083d1d-dc8b-4ea0-91fa-8744cf0f013b among choices: http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient
        Unable to find a match for profile Patient/10083d1d-dc8b-4ea0-91fa-8744cf0f013b among choices: http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient
        Unable to find a match for profile Patient/10083d1d-dc8b-4ea0-91fa-8744cf0f013b among choices: http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient
    TERMINOLOGY_TX_SYSTEM_WRONG_HTML:1
        The code system reference https://hl7.org/fhir/R4/valueset-gender-identity.html is wrong - the code system reference cannot be to an HTML page. This may be the correct reference: {unable to determine intended url}
    SLICING_CANNOT_BE_EVALUATED:3
        Slicing cannot be evaluated: Problem with use of resolve() - profile [CanonicalType[http://hl7.org/fhir/StructureDefinition/individual-genderIdentity]] on Patient.extension:individualGenderIdentity could not be resolved (@char 1)
        Slicing cannot be evaluated: Problem with use of resolve() - profile [CanonicalType[http://hl7.org/fhir/StructureDefinition/individual-genderIdentity]] on Patient.extension:individualGenderIdentity could not be resolved (@char 1)
        Slicing cannot be evaluated: Problem with use of resolve() - profile [CanonicalType[http://hl7.org/fhir/StructureDefinition/individual-genderIdentity]] on Patient.extension:individualGenderIdentity could not be resolved (@char 1)
    BUNDLE_ENTRY_URL_ABSOLUTE:17
        The fullUrl must be an absolute URL (not 'MessageHeader/16e3e628-2a6d-4d73-a854-b5a7b7c0b634')
        The fullUrl must be an absolute URL (not 'Provenance/bf58460f-6f26-4844-8edb-5ab6f98a0385')
        The fullUrl must be an absolute URL (not 'DiagnosticReport/3268e1d6-cb84-459b-8640-e7930369d34d')
        The fullUrl must be an absolute URL (not 'Patient/10083d1d-dc8b-4ea0-91fa-8744cf0f013b')
        The fullUrl must be an absolute URL (not 'Organization/719ec8ad-cf59-405a-9832-c4065945c130')
        The fullUrl must be an absolute URL (not 'Practitioner/ee29ccf5-631d-4b35-a6d4-30a61c0eb8d9')
        The fullUrl must be an absolute URL (not 'Specimen/baf09fcd-95d1-48d7-9b12-3f52d57d69f4')
        The fullUrl must be an absolute URL (not 'ServiceRequest/66e1f9cb-aa34-48a8-80b7-495b7d0c3a9f')
        The fullUrl must be an absolute URL (not 'Device/989c07bb-de54-4205-b38e-a5b4b08595d7')
        The fullUrl must be an absolute URL (not 'PractitionerRole/e476cc45-7711-4a24-9e5e-c28be0d10b44')
        The fullUrl must be an absolute URL (not 'Organization/07640c5d-87cd-488b-9343-a226c5166539')
        The fullUrl must be an absolute URL (not 'Observation/f08d453c-b80d-4fe3-83cd-c2fc55d9afeb')
        The fullUrl must be an absolute URL (not 'Observation/f9d62f1e-664f-3d44-9af7-d8002aecc319')
        The fullUrl must be an absolute URL (not 'Observation/c890b146-7cd8-3d26-ac8e-ae22b89996ec')
        The fullUrl must be an absolute URL (not 'Observation/6ba7c37f-fb67-32df-91d4-0fdbf7b71956')
        The fullUrl must be an absolute URL (not 'Observation/dfbd2c06-0bb9-3b87-952e-cbeb213f2f43')
        The fullUrl must be an absolute URL (not 'Observation/d750dcec-a11a-3202-b734-b291c798ae20')


Warnings
    Constraint:17
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
    The Coding:1
        The Coding provided (http://terminology.hl7.org/CodeSystem/v2-0003#R01) was not found in the value set 'US Public Health VaueSet - Message Types' (http://hl7.org/fhir/us/ph-library/ValueSet/us-ph-valueset-message-types|1.0.0), and a code should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable).  (error message = Unknown code 'http://terminology.hl7.org/CodeSystem/v2-0003#R01' for in-memory expansion of ValueSet 'http://hl7.org/fhir/us/ph-library/ValueSet/us-ph-valueset-message-types')
    None of th:18
        None of the codings provided are in the value set 'Provenance activity type' (http://hl7.org/fhir/ValueSet/provenance-activity-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://terminology.hl7.org/CodeSystem/v2-0003#R01)
        None of the codings provided are in the value set 'US Core Laboratory Test Codes' (http://hl7.org/fhir/us/core/ValueSet/us-core-laboratory-test-codes|7.0.0), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#94531-1)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://terminology.hl7.org/CodeSystem/v2-0301#CLIA)
        None of the codings provided are in the value set 'SNOMED CT Body Structures' (http://hl7.org/fhir/ValueSet/body-site|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://snomed.info/sct#87100004)
        None of the codings provided are in the value set 'US Core Procedure Codes' (http://hl7.org/fhir/us/core/ValueSet/us-core-procedure-code|7.0.0), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#94531-1)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = null#MNI)
        None of the codings provided are in the value set 'Observation Interpretation Codes' (http://hl7.org/fhir/ValueSet/observation-interpretation|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://terminology.hl7.org/CodeSystem/v2-0078#A)
        None of the codings provided are in the value set 'LOINC Codes' (http://hl7.org/fhir/ValueSet/observation-codes|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#95406-5)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
        None of the codings provided are in the value set 'LOINC Codes' (http://hl7.org/fhir/ValueSet/observation-codes|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#95419-8)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
        None of the codings provided are in the value set 'LOINC Codes' (http://hl7.org/fhir/ValueSet/observation-codes|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#11368-8)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
        None of the codings provided are in the value set 'LOINC Codes' (http://hl7.org/fhir/ValueSet/observation-codes|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#82810-3)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
        None of the codings provided are in the value set 'LOINC Codes' (http://hl7.org/fhir/ValueSet/observation-codes|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#95418-0)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
        None of the codings provided are in the value set 'LOINC Codes' (http://hl7.org/fhir/ValueSet/observation-codes|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#95421-4)
    Unable to :9
        Unable to expand ValueSet because CodeSystem could not be found: http://loinc.org
        Unable to expand ValueSet because CodeSystem has CodeSystem.content=not-present but contents were not found: http://snomed.info/sct
        Unable to expand ValueSet because CodeSystem could not be found: http://loinc.org
        Unable to expand ValueSet because CodeSystem could not be found: http://loinc.org
        Unable to expand ValueSet because CodeSystem could not be found: http://loinc.org
        Unable to expand ValueSet because CodeSystem could not be found: http://loinc.org
        Unable to expand ValueSet because CodeSystem could not be found: http://loinc.org
        Unable to expand ValueSet because CodeSystem could not be found: http://loinc.org
        Unable to expand ValueSet because CodeSystem could not be found: http://loinc.org
    ValueSet ':1
        ValueSet 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1099.54' not found
    Coding has:2
        Coding has no system. A code with no system has no defined meaning, and it cannot be validated. A system should be provided
        Coding has no system. A code with no system has no defined meaning, and it cannot be validated. A system should be provided
    Best Pract:22
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
    A code wit:1
        A code with no system has no defined meaning, and it cannot be validated. A system should be provided




Validating resource: /fhirsamples/SR-bundle-fixed.fhir.json addProfiles: false
Done validating resource. Time: 38.782750ms
There are 0 errors, 52 warnings, and 10 information notes

Errors
None


Warnings
    Constraint:17
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
        Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)
    None of th:9
        None of the codings provided are in the value set 'Provenance activity type' (http://hl7.org/fhir/ValueSet/provenance-activity-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://terminology.hl7.org/CodeSystem/v2-0003#R01)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://terminology.hl7.org/CodeSystem/v2-0301#CLIA)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = null#MNI)
        None of the codings provided are in the value set 'Observation Interpretation Codes' (http://hl7.org/fhir/ValueSet/observation-interpretation|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://terminology.hl7.org/CodeSystem/v2-0078#A)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
        None of the codings provided are in the value set 'IdentifierType' (http://hl7.org/fhir/ValueSet/identifier-type|4.0.1), and a coding should come from this value set unless it has no suitable code (note that the validator cannot judge what is suitable) (codes = http://loinc.org#81959-9)
    Unable to :1
        Unable to expand ValueSet because CodeSystem could not be found: http://loinc.org
    Coding has:2
        Coding has no system. A code with no system has no defined meaning, and it cannot be validated. A system should be provided
        Coding has no system. A code with no system has no defined meaning, and it cannot be validated. A system should be provided
    Best Pract:22
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
        Best Practice Recommendation: In general, all observations should have a performer
        Best Practice Recommendation: In general, all observations should have an effective[x] ()
    A code wit:1
        A code with no system has no defined meaning, and it cannot be validated. A system should be provided

Process finished with exit code 0
