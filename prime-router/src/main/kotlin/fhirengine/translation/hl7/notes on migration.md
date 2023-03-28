The code in this folder is targeted to be migrated to the
[PRIME FHIR Converter](https://github.com/CDCgov/prime-fhir-converter) repository.

# Rules

1. All the library code is updated in this location of
   the [PRIME ReportStream](https://github.com/CDCgov/prime-data-hub)
   repository until we are ready to migrate ReportStream to use the library instead of the code here.
2. All code in the library must be isolated from the rest of ReportStream. There shall be no ReportStream specific
   code in the library.
3. Code must be fully documented, tested and stable.

# How to copy the code to the library repo

Note: These are the steps taken to copy code from Feb 3, 2023. Copying newer code may require changes to this list.

1. Create a new branch in the [PRIME FHIR Converter](https://github.com/CDCgov/prime-fhir-converter) repository.
2. In the new branch. delete all the files and folders under the following folders. This will make sure we are not
   keeping old files.
    1. `src/main/kotlin/gov/cdc/prime/fhirconverter/translation/hl7`
    2. `src/main/resources/hl7_mapping`
    3. `src/test/kotlin/gov/cdc/prime/fhirconverter/translation/hl7`
    4. `src/test/resources/schema`
3. Copy the files from the [PRIME ReportStream](https://github.com/CDCgov/prime-data-hub) to the
   [PRIME FHIR Converter](https://github.com/CDCgov/prime-fhir-converter) as follows:
    1. Copy `prime-router/src/main/kotlin/fhirengine/translation/hl7/*` from
       [PRIME ReportStream](https://github.com/CDCgov/prime-data-hub) to
       `src/main/kotlin/gov/cdc/prime/fhirconverter/translation/hl7`
    2. Copy `prime-router/metadata/hl7_mapping/ORU_R01/*` from
       [PRIME ReportStream](https://github.com/CDCgov/prime-data-hub) to
       `src/main/resources/hl7_mapping/ORU_R01`. Also consider copying any new mappings that are generic (e.g.
       not for a given receiver)
    3. Copy `prime-router/src/test/kotlin/fhirengine/translation/hl7/*` from
       [PRIME ReportStream](https://github.com/CDCgov/prime-data-hub) to
       `src/test/kotlin/gov/cdc/prime/fhirconverter/translation/hl7`
    4. Copy `prime-router/src/test/resources/fhirengine/translation/hl7/schema/*` from
       [PRIME ReportStream](https://github.com/CDCgov/prime-data-hub) to
       `src/test/resources/schema`
4. In the [PRIME FHIR Converter](https://github.com/CDCgov/prime-fhir-converter) repository, replace globally
    1. the string `gov.cdc.prime.router.fhirengine.translation.hl7` with `gov.cdc.prime.fhirconverter.translation.hl7`
    2. the string `src/test/resources/fhirengine/translation/hl7/schema` with `src/test/resources/schema`
5. Open the `build.gradle.kts` on both repositories and update the library versions used in
   [PRIME FHIR Converter](https://github.com/CDCgov/prime-fhir-converter) to be the same version used in
   [PRIME ReportStream](https://github.com/CDCgov/prime-data-hub) `prime-router` folder.
6. In the [PRIME FHIR Converter](https://github.com/CDCgov/prime-fhir-converter) repository, run a
   `./gradle build` and verify the build passes. If there are errors you will need to debug the issue and match what
   is in [PRIME ReportStream](https://github.com/CDCgov/prime-data-hub). Do not make custom code changes in the library.