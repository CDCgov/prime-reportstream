# How to use ReportStream Filters

Filters are ReportStream's powerful mechanism for deciding which Items go to which receivers.  (In ReportStream, an 'Item' is a single row in a CSV, or a single complete HL7 Message)

### Filter Types

ReportStream provides a library of filters - tools that can be used to prevent an Item with certain values in certain fields from going to a Receiver.

The library of available filters is defined in code right now, in ReportStreamFilterDefinition.kt

For example, as of this writing these filters are defined:
        FilterByCounty(),
        Matches(),
        DoesNotMatch(),
        OrEquals(),
        HasValidDataFor(),
        HasAtLeastOneOf(),
        AllowAll(),
        IsValidCLIA(),


