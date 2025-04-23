# Validating FHIR documents

# Running FHIR validation
There is a class called gov.cdc.prime.router.fhirvalidation.RSFhirValidator that implements FHIR validation.
There is an example of how to use it in gov.cdc.prime.router.fhirvalidation.FhirValidatorTests. Basically
you get create a validator, then call validateResource. validateResource can take a FHIR Resource object, or a URL.
If you want to validate a file in the resources dir, use validateFhirInResourcesDir. All these functions take
an optional addProfiles parameter that specifies whether or not to add meta.profile elements to RS resources.
Without these resources, it will only validate against FHIR R4.

Example:
`
val validator = RSFhirValidator()
val result = validator.validateFhirInResourcesDir("/fhirsamples/SR-bundle-original.fhir.json")

## Samples
There are samples in prime-router/src/test/resources/fhirsamples.
SR-bundle-original.fhir.json is the original SR fhir.
SR-bundle-fixed.fhir.json has been updated to pass FHIR R4
SR-bundle-with-proifles.fhir.json is the original with meta.profiles added

## Utility
There is a utility called addFhirProfiles that will add meta.profile elements to fhir files.

# Unit test
Currently, there is one unit test that runs two validations. One without profiles, which means it only validates
against the base FHIR R4 profile. The second test includes profiles so it validates against all the identified 
profiles from US Public Health Profile Library (USPHPL) and US Core. 

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


# Detailed validation output
```
Validating resource: SR-bundle.fhir.json
Done validating resource. Time: 2.046355500s
There are 19 errors, 52 warnings, and 18 informations

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



*********************




Validating resource: SR-bundle-with-profiles.fhir.json
Done validating resource. Time: 121.121750ms
There are 59 errors, 71 warnings, and 711 informations

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
```