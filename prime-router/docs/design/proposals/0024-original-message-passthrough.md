# Proposal for Original Message Passthrough

## Background

In order to meet data integrity requirements, a sender of HL7v2 has requested a feature in the Universal Pipeline to
preserve the original unaltered message and convey it to receivers with no alterations, including preserving the
original file name.

## Assumptions

This design is strictly for an HL7v2 sender and does not apply to a FHIR sender. Likewise, this does not apply to a FHIR
receiver.

## Possible Implementations

We will need to store enough information in the pipeline to:

* Identify an item as an original passthrough item
* Reproduce the content of the original item

The following proposed implementations can be grouped into two major approaches: storing the HL7v2 data within the FHIR
bundle, or utilizing the blobURL of the original item upload.

### Store HL7v2 in custom FHIR extension

This set of implementations seeks to store the entire content of the incoming HL7v2 file within the FHIR bundle as a
custom extension when the incoming file is received, after which the stored data would be used to reproduce the file.
The custom extension is able to store the sender setting specifying original passthrough, the incoming HL7v2 filename,
and the body of the file.

#### Pros

* The HL7v2 message in its entirety can be transmitted even to a FHIR receiver, should a receiver wish to see the source
  file.
    * It should be noted that if we elect to store the HL7v2 message for all incoming files that the message would not
      reflect any filtering that would occur for a FHIR receiver.

#### Cons

* At a minimum, this will require adjusting the original HL7v2 message to serialize newline characters as `\n` to allow
  the message to be stored in the FHIR bundle. This is a lossless encoding that will need to be handled when exporting
  the file.

#### Implementations

* **Add feature to the FHIR converter library**:

  Modify the CDCgov fork of the `hl7v2-fhir-converter` library to store the original filename and body of the input HL7
  message inside the FHIR bundle as an extension within the bundle's Provenance resource.
    * *Pro*: Storing the HL7v2 file body becomes automatic with minimal code change, as the FHIR converter library
      already receives the HL7v2 body to process it.
    * *Con*: The converter library currently does not receive the submission's original filename. We will need a means
      of supplying this to the library.
        * To consider: The FHIR converter is not intended to be specific to ReportStream and as such, any extensions to
          it should be generic (e.g. pass in a settings JSON object to be included rather than creating settings
          specific to ReportStream as named parameters).

* **Add feature to the UP as a bundle enhancement**:

  Add the original filename and the entire body of the received HL7v2 file to the FHIR bundle as an extension within the
  bundle's Provenance resource.

    * *Pro*: Unlike handling this feature in the FHIR converter library, the original HL7v2 filename can be more easily
      made available when performing a bundle enhancement to add the extension.
    * *Con*:

### Retrieve the HL7v2 file from Azure blobstore

The incoming HL7v2 file is stored to the Azure blobstore as part of the `receive` pipeline step. This can be utilized to
extract the original HL7v2 file from the blobstore.

#### Pros

* This approach makes use of the existing copy of the original submission in the Azure blobstore. This guarantees a byte
  for byte copy of the original submission as well as eliminating the need for an additional location to store the
  original submission data.

#### Cons

* In this approach, the original file body is stored only in ReportStream's Azure blobstore, and will not be available
  to downstream FHIR receivers should they wish to be able to view it in the future.

#### Implementations

* **Add feature to the UP as a bundle enhancement**:

  We can introduce enhancing the FHIR bundle created in the `convert` pipeline step to add the sender setting specifying
  original passthrough, the incoming HL7v2 filename, and the blobURL, as an extension within the bundle's Provenance
  resource.

    * *Con*: We need to be mindful of security concerns if URIs specific to ReportStream's Azure blobstore are to be
      stored in the FHIR bundle. We may need to deidentify the URI to remove any sensitive information or strip this
      information from the FHIR bundle when preparing to send it if a FHIR receiver is configured to receive it.

* **Add feature to the UP translate step: retrieve blobURL from report_file DB table**:

  In this approach, the process of creating the FHIR bundle is not altered in any way. Instead, the `translate` step is
  altered to determine if original message passthrough is configured. If so, the `translate` step can then walk through
  the `report_file` database table in a similar manner as done in the Submission History API to retrieve the blobURL and
  the externalName of the original HL7v2 file and make a copy of it to send to the receiver.
    * As information is not being added to the FHIR bundle, the determination that original passthrough is desired must
      be made another way. This can take several forms, such as introducing a new `Report.Format` (e.g. `HL7_ORIGINAL`)
      or having the `translate` function retrieve sender settings for the original submission.

    * *Pro*: This approach potentially has the least amount of modifications to existing processes and makes use of
      information that is already stored in the database. There is no need to store additional information about the
      submission to facilitate the passthrough feature.
    * *Pro*: There is no risk of leaking private information about ReportStream's storage as the FHIR bundle is not
      modified.
    * *Con*: This approach is highly reliant on the processes performed at the `receive` pipeline step remaining as they
      are now. Any refactoring of this code must take original message passthrough into consideration.

## Limitations

* Filtering can still occur as a routing process, but the nature of sending an unmodified HL7v2 file will mean any
  conditions that would have been eliminated by filtering would be sent regardless.
* `MERGE` batching cannot be performed for original passthrough HL7v2 files.
* Filenames in the Azure blobstore currently reflect pipeline specific information to more easily identify when and why
  a file was created.
    * There is currently a limitation that files must have unique names, and blobURLs cannot be reused.
    * External filenames are in many cases based on the blobURL presently.

## Questions

* Will original passthrough utilize existing receiver filtering, or should we require a receiver specific to receiving
  original passthrough files?
    * If we utilize existing receivers, does the sender flag indicating original passthrough override receiver quality
      filters?
    * Alternatively, should a new `Report.Format` (e.g. `HL7_ORIGINAL`) be added to flag original passthrough items?
