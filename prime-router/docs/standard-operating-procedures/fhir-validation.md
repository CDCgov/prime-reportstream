# FHIR Validation
ReportStream senders and receivers can either use HL7v2 or FHIR for ELR reports. This document specifies exactly what
data is required for sending FHIR ELR reports to ReportStream and what to expect when receiving FHIR ELR reports. It
specifies which FHIR standards ReportStream conforms to. Validation is the process of testing
how FHIR resources conform to specified standards.

# FHIR Profiles
RS conforms to the [FHIR Messaging](https://build.fhir.org/messaging.html) standard and uses
profiles from the [US Public Health Profiles Library](https://build.fhir.org/ig/HL7/fhir-us-ph-common-library-ig) v1.0.0
when possible. If an USPHPL profile doesn't exist for a resource type, it uses profiles from
[US Core Implementation Guide](https://hl7.org/fhir/us/core/index.html) v7.0.0. If an appropriate profile doesn't
exist in US Core, it defaults to [FHIR R4](https://hl7.org/fhir/R4/).

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
Currently, RS doesn't perform FHIR validation. The validation code is ready to be used. There is a fhir-validation CLI
command. There are known non-conformances issues with SimpleReport inputs as well as internally
stored FHIR documents. In the future we intend to perform FHIR validation when a sender submits a FHIR ELR report and return
an error response if it isn't valid. We also intend to store and send valid FHIR in the future. These efforts have been
put on hold.

# FHIR validation CLI
A CLI command has been implemented that performs FHIR validation. It can be invoked from the prime-reportstream
directory using a command like:
``bash
./gradlew primeCLI --args='validate-fhir --file /myrepo/prime-router/src/test/resources/fhirsamples/SR-bundle-original.fhir.json'
``

The CLI implementation is gov.cdc.prime.router.cli.ValidateFHIRCommand and there is a unit test here: 
gov.cdc.prime.router.cli.ValidateFHIRCommandTests

# FHIR validation implementation
There is a class called gov.cdc.prime.router.fhirvalidation.RSFhirValidator that implements FHIR validation.
There is an example of how to use it in gov.cdc.prime.router.fhirvalidation.FhirValidatorTests. Basically
you create a validator, then call validateResource. validateResource can take a FHIR Resource object, or a URL
to a FHIR document.
If you want to validate a file in the resources dir, use validateFhirInResourcesDir. All these functions take
an optional addProfiles parameter that specifies whether or not to add meta.profile elements to RS resources.
Without these resources, it will only validate against FHIR R4.

Using RSFhirValidator:
`
val validator = RSFhirValidator()
val result = validator.validateFhirInResourcesDir("/fhirsamples/SR-bundle-original.fhir.json")
validator.printResults(result)
`

# Location of profiles
There are two zip files in src/main/resources: hl7.fhir.us.core-7.0.0.tgz and usphpl-package.r4.tgz
These contain all the conformance resources from 
[US Core Implementation Guide](http://hl7.org/fhir/us/core/ImplementationGuide/hl7.fhir.us.core) version 7.0.0 
and [US Public Health Profiles Library](http://hl7.org/fhir/us/ph-library/ImplementationGuide/hl7.fhir.us.ph-library) 
version 1.0.0.

## Samples
".fhir.json" is the preferred filename suffix. That way Intellij knows it's a JSON file and does syntax highlighting
and code folding. Also you'll notice that one of the sample files ends in ".fhir.json5". JSON5 is a standard that is basically JSON with
comments. FHIR can be very complicated, and it's very useful to be able to put comments in the JSON. Intellij knows
how to handle JSON5 and RSFhirValidator.validateResource() knows how to deal with JSON5. It converts it to regular
JSON before parsing.

The following sample FHIR files are in prime-router/src/test/resources:
* simple-patient.fhir.json - a very basic FHIR resource
* simple-patient.fhir.json.json - The above with meta/profile added by the addFhirProfiles utility defined in RSFhirValidator.
* SR-bundle-original.fhir.json - an unmodified SimpleReport FHIR sample report
* SR-bundle-fixed.fhir.json - the above but fixed to pass base FHIR r4
* SR-bundle-with-profiles.fhir.json - the above with meta/profiles added
* SR-bundle-fixed-full.fhir.json5 - the beginnings of a version of the above that is intended to eventually 
pass all the required USPHPL and US Core profiles. It still needs work.

## Utility
There is a utility called addFhirProfiles that will add meta.profile elements to FHIR files. This lets you add RS
profiles to FHIR files that don't contain meta/profile elements. The modified file name has an additional ".json"
appended. If meta/profiles already exists, it leaves it alone.

# Unit tests
These are here: gov.cdc.prime.router.fhirvalidation.FhirValidatorTests.
Currently, there are three unit tests that run validations on some of the samples in
prime-router/src/test/resources/fhirsamples. The first one runs on SR-bundle-original.fhir.json with and
without profiles. The second test runs SR-bundle-fixed.fhir.json which passes FHIR R4.
The third test runs on SR-bundle-fixed-full.fhir.json5. Which is intended to pass validation
against all the identified profiles from US Public Health Profile Library (USPHPL) and US Core.


I ran the tests directly from Intellij. It should be possible to run it from the command line using
```
./gradlew test --tests gov.cdc.prime.router.fhirvalidation.FhirValidatorTests
```
But I got the following error. I need to figure out why.
```
> Task :auth:test FAILED
```

## Using profiles
Each type of FHIR resource is validated against a specific profile. It is common practice to specify profiles
in the meta/profile element of each resource. There is an example of this in SR-bundle-with-profiles.fhir.json.
If a profile is not specified, RSFhirValidator will use the profiles from the above table. The source of truth
is specified in the type2url variable of RSFhirValidator which maps FHIR resource types to canonical urls of
structure definitions. When calling RSFhirValidator.validateResource, there is a parameter which specifies
whether or not to add profiles. It is true by default. If it is false, then validation is against the base FHIR
R4 profiles.

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

# Validation Notes
The HAPI FHIR validation configuration is implemented in gov.cdc.prime.router.fhirvalidation.RSFhirValidator.
It uses two packages of configuration resources that are in the test/resources directory. 
(They will be in the main/resources directory):<br />
hl7.fhir.us.core-7.0.0.tgz<br />
usphpl-package.r4.tgz<br />



