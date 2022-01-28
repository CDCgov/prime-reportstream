#  ReportStream Release Notes

*January 28, 2022*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Support for new date and date/time formats for CSV reports

ReportStream now supports the following two new formats for date and date/time fields in CSV reports:
- `M/d/yyyy[ HH:mm[:ss[.S[S][S]]]]`
- `yyyy/M/d[ HH:mm[:ss[.S[S][S]]]]`

The current list of supported date and date/time formats is:
- `yyyyMMdd`
- `yyyyMMddHHmmZZZ`
- `yyyyMMddHHmmZ`
- `yyyyMMddHHmmss`
- `yyyy-MM-dd HH:mm:ss.ZZZ`
- `yyyy-MM-dd[ HH:mm:ss[.S[S][S]]]`
- `yyyyMMdd[ HH:mm:ss[.S[S][S]]]`
- `M/d/yyyy[ HH:mm[:ss[.S[S][S]]]]`
- `yyyy/M/d[ HH:mm[:ss[.S[S][S]]]]`
- `MMddyyyy`




