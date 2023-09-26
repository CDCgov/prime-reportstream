# Pipeline Configuration

This section explains how a ReportStream engineer will use and configure the pipeline.

## Difference between COVID & Universal Pipeline Configurations

The COVID pipeline was developed to be flexible and meet senders and receivers where they were in order to meet an
immediate need. In comparison, the Universal Pipeline is built around adherence to data standards in order to represent
a wider range of reportable conditions. This results in fewer customization options, which leads to a leaner, more
narrow configuration.

In general terms, the primary means of data manipulation in the Universal Pipeline is accomplished through sender and
receiver transforms, where data can be copied and manipulated in a rules-based manner.

## Organization Configuration

Consult the following sections to see configuration options for Universal Pipeline senders and receivers:

### Senders
* [Convert step](../universal-pipeline/convert.md)

### Receivers
* [Translate step](../universal-pipeline/translate.md)
* [Batch step](../universal-pipeline/batch.md)
* [Send step](../universal-pipeline/send.md)

## How to create and manage a sender or receiver transform

Sender and receiver transforms allow for data meeting certain criteria to be transformed. These transforms are performed
either when preparing FHIR data (converted from HL7 if necessary) from a sender to be stored within the pipeline's
internal FHIR data structure (a sender transform) or when preparing data to be transmitted to a receiver
(a receiver transform).

Sender and receiver transform schemas are currently stored in the project filesystem. (This will change as new features
are developed.) These schemas control what information is selected for transformation and what transforms are performed.
Sender and receiver transforms both use a `.yml` (YAML) format.

### Defining Sender Transforms

Sender transform schemas are currently stored in `metadata/fhir_transforms/senders` and are utilized by a given sender when
defining the `schemaName` element in the sender's settings. See this example where the `simple-report-sender-transform`
is defined for a sender named `dev-fhir`:

```
    - name: dev-fhir
      schemaName: metadata/fhir_transforms/senders/SimpleReport/simple-report-sender-transform
      format: FHIR
```

Most senders are configured to use the default sender transform, which inserts ReportStream specific values into the
data. Schemas are designed to be extensible. Transforms created for a specific sender would typically extend the default
transform while adding additional transforms needed for the sender. The `simple-report-sender-transform` schema,
accordingly, extends the default transform as well.

> For more context on how sender transforms are used, please reference [Convert](../universal-pipeline/convert.md).

### Defining Receiver Transforms

Receiver transform schemas are currently stored in `metadata/hl7_mapping/receivers` and are utilized by a given receiver when
defining the `schemaName` element within the `translation` settings for the receiver. See this example where the
`CA-receiver-transform` is defined for a receiver named `CA_FULL_ELR`:

```
    - name: CA_FULL_ELR
      translation: !<HL7>
        schemaName: "metadata/hl7_mapping/receivers/STLTs/CA/CA-receiver-transform"
```

> For more context on how receiver transforms are used, please reference [Translate](../universal-pipeline/translate.md).

Receiver transform schemas, in addition to transforming values, can also define HL7v2 mappings as needed for a specific receiver.

 > For more details on when a transform should be defined as a sender or receiver transform, and for details on how the
transform schemas are laid out, please reference
> [Changing/Updating Sender/Receiver Transforms](./standard-operating-procedures/changing-transforms.md) for guidance.