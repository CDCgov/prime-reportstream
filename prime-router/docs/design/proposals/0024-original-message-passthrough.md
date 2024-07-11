# Proposal for sendOriginal Pipeline Function

## Background

In order to meet data integrity requirements, a sender has requested a feature in the Universal Pipeline to
preserve the original unaltered message and convey it to receivers with no alterations, including preserving the
original file name.

## Assumptions

* While this design is intended to support a specific use case, CDC ELIMS ETOR, it should be easily applicable to other
  use cases as well and fit into the existing architecture of the pipeline.
* Senders will send using a message format supported by the Universal Pipeline (currently HL7v2 and FHIR).
* Senders using the sendOriginal functionality shall NOT send batched messages. Each submitted report shall contain only
  one "message", otherwise a receiver may receive duplicate messages.

## Possible Implementations

We will need to store enough information in the pipeline to:

* Identify an item as an sendOriginal item
* Reproduce the filename and content of the original item

This information would be made available to the `translate` step of the UP, which would then be changed to retrieve the
original item instead of performing receiver translation.

The report API currently accepts a filename from a sender using the `payloadname` request header or query string
parameter. This can be utilized to capture the filename to use when sending the report to receivers.

The following proposed implementations can be grouped into two major approaches: storing the HL7v2 data within the FHIR
bundle (in the case of an HL7v2 input message), or utilizing the blobURL of the original item upload. Details for each
of these approaches follow. In summary:

| Feature Implementation                               | Original message available to FHIR (non sendOriginal) receivers | Requires additional storage | Requires convert step changes | Requires translate step changes |
|------------------------------------------------------|-----------------------------------------------------------------|-----------------------------|-------------------------------|---------------------------------|
| Store body in bundle (add feature to FHIR converter) | Yes                                                             | Yes                         | Yes                           | Yes                             |
| Store body in bundle (bundle enhancement)            | Yes                                                             | Yes                         | Yes                           | Yes                             |
| Store blobURL in bundle (bundle enhancement)         | No                                                              | Yes                         | No                            | Yes                             |
| Retrieve blobURL from database                       | No                                                              | No                          | No                            | Yes                             |

### Store body of file in custom FHIR extension

This set of implementations seeks to store the entire content of the incoming file within the FHIR bundle as a
custom extension when the incoming file is received, after which the stored data would be used to reproduce the file.
The custom extension is able to store the sender setting specifying sendOriginal, the incoming filename,
and the body of the file.

#### Pros

* The original message in its entirety can be transmitted even to a FHIR receiver, should a receiver wish to see the
  source file.
    * It should be noted that if we elect to store the message for all incoming files that the message would not
      reflect any filtering that would occur for a FHIR receiver.

#### Cons

* At a minimum, this will require adjusting the original message to serialize newline characters as `\n` to allow
  the message to be stored in the FHIR bundle. This is a lossless encoding that will need to be handled when exporting
  the file.
* This approach limits the possible message formats to HL7v2 only; we would not be able to perform an sendOriginal
  passthrough of a FHIR bundle or filetypes not currently supported by the Universal Pipeline.

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

### Retrieve the original file from Azure blobstore

The incoming file is stored to the Azure blobstore as part of the `receive` pipeline step. This can be utilized to
extract the original file from the blobstore.

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
  sendOriginal, the incoming HL7v2 filename, and the blobURL, as an extension within the bundle's Provenance
  resource.
    * *Con*: We need to be mindful of security concerns if URIs specific to ReportStream's Azure blobstore are to be
      stored in the FHIR bundle. We may need to deidentify the URI to remove any sensitive information or strip this
      information from the FHIR bundle when preparing to send it if a FHIR receiver is configured to receive it.

* **Add feature to the UP translate step: retrieve blobURL from report_file DB table**:

  In this approach, the process of creating the FHIR bundle is not altered in any way. Instead, the `translate` step is
  altered to determine if sendOriginal is configured. If so, the `translate` step can then walk through
  the `report_file` database table in a similar manner as done in the Submission History API to retrieve the blobURL and
  the externalName of the original HL7v2 file and make a copy of it to send to the receiver.
    * As information is not being added to the FHIR bundle, the determination that sendOriginal is desired must
      be made another way. Aside from a topic feature flag, this can take several other forms such as introducing a new
      `MimeFormat` (e.g. `HL7_ORIGINAL`) or having the `translate` function retrieve sender settings for the original
      submission.

    * *Pro*: This approach potentially has the least amount of modifications to existing processes and makes use of
      information that is already stored in the database. There is no need to store additional information about the
      submission to facilitate the sendOriginal feature.
    * *Pro*: There is no risk of leaking private information about ReportStream's storage as the FHIR bundle is not
      modified.
    * *Con*: This approach is highly reliant on the processes performed at the `receive` pipeline step remaining as they
      are now. Any refactoring of this code must take sendOriginal into consideration.

## Limitations

* Filtering can still occur as a routing process, but the nature of sending an unmodified HL7v2 file will mean any
  conditions that would have been eliminated by filtering would be sent regardless.
* Batching cannot be performed for sendOriginal files.
* Filenames in the Azure blobstore currently reflect pipeline specific information to more easily identify when and why
  a file was created.
    * There is currently a limitation that files must have unique names, and blobURLs cannot be reused.
    * External filenames are in many cases based on the blobURL presently.

## Feature Design

After performing analysis of the possible implementations above, the **Add feature to the UP translate step: retrieve
blobURL from report_file DB table** implementation was selected.

The overall design of the sendOriginal feature is as follows:

* A new `Topic` feature flag, `sendOriginal`, will be used for sendOriginal items. The use of Topic to define
  sendOriginal guarantees that data that must be handled via sendOriginal is not processed as other
  full ELR data and is only sent to receivers in its original state. This also allows receivers to be able to configure
  endpoints specific to the data for this particular sender, separate from any batching settings the receiver may
  otherwise have configured.
* As the `Topic` is unique, no other identifiers for sendOriginal items are needed. Configuring the sender and
  receivers for the Topic with `sendOriginal` defined is all that is required.
* The `translate` step will copy the original blob data from the blobstore instead of translating from the internal
  FHIR bundle. Enhancements to `BlobAccess` that have been made in preparation for the `Manage Translation Schemas`
  feature can be utilized to perform this operation. The `translate` step will then directly insert the resulting blob
  to the `send` queue.
* `Report.formExternalFilename` uses the filename of the blobstore item, so there should not be any further
  modifications necessary as long as the blobstore item is named correctly by the `translate` function.

### Issues to be created

* Add header to `receive` step to allow sender to supply original filename and store in database
  field `report_file.external_name`.
* Implement the sendOriginal Topic setting, write function to look up original filename and blob URL for a given report
  ID, add step to `translate` function to check message topic, copy the original blob with original filename (with
  report ID appended), and insert into send queue.
    * The database currently requires blobURLs to be unique. Appending the report ID to the original filename should
      satisfy this requirement. Another approach to guaranteeing the blobURL remains unique is to add a virtual
      directory to the blobURL that uniquely identifies the receiver. The existing process to retrieve the filename
      based on blobURL should still function as expected.
* Optionally, add a check to the `receive` step to reject submissions in cases where only one report is expected but
  multiple reports are received.