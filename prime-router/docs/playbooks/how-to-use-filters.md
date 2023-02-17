# How to use ReportStream Filters

Filters are ReportStream's powerful generic mechanism for deciding which Items go to which receivers.  (In ReportStream, an _Item_ is a single row in a CSV, or a single complete HL7 Message.  For example, with covid-19 data, an _Item_ is a single Covid-19 test result.)

### Our Filter Library

ReportStream provides a library of filters - tools that can be used to prevent an Item with certain values in certain fields from going to a Receiver.

The library of available filters is defined and documented in code right now, in ReportStreamFilterDefinition.kt

For example, as of this writing these filter functions are defined in our library:

- filterByCounty
- matches
- doesNotMatch
- orEquals
- hasValidDataFor
- hasAtLeastOneOf
- atLeastOneHasValue  
- allowAll
- allowNone
- isValidCLIA

### Logging of Filters applied

If a filter is applied, information on what happened will appear in the submission history.   Currently the history of an applied filter looks like this:

```
For dc-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] reduced the item count from 1 to 0.  Data with these IDs were filtered out: (ABC-123)
```

The IDs listed (eg, ABC-123 in the example) are the trackingElement values of the items _eliminated_ from being sent to that receiver, _not_ the items that remain.  To find out which field is being used as the trackingElement, look at the top of the schema.   If no trackingElement is specified, or if the sender failed to supply a value for the field, then the message is shortened to:

```
For dc-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] reduced the item count from 1 to 0.
```

### How filtering works

As of this writing, ReportStream applies four rounds of filtering rules, in this order

1. jurisdictionalFilter
2. qualityFilter
3. routingFilter
4. processingModeFilter

The code that does filtering is in Translator.kt.

All the filters work the same way, with a few minor exceptions noted in the next section.

For each filter type above, you can call filter functions at three levels:
1. Global Defaults, set per `topic`.  As of this writing, these are set in the code, in ReportStreamFilter.kt
2. Per Organization, set per `topic`
3. Per Receiver (a receiver can only operate on one topic)

- At each of those levels, you can make zero or more calls to filter functions.
- Filters are applied per item of data, per receiver.
- The Global Default is only applied if no Organization or Receiver level rules exist.  That is, any Organization or Receiver rules cause the Global default to be overridden and not used.
- The Organization and Receiver level filters are "and"ed together and all those rules are applied.  That is, Receivers do not override Organization filters, but they can add further stringency by doing additional filtering.
- Data items only go to a receiver if it makes it through all four filters.
- If an item fails a filter, that's the end - we don't try further filters.   

To summarize, filters are per `topic`, there are four filter types, each can be specified in three levels, and each of those can call zero or more filter functions, which can operate on any fields in the data.

### Special cases

The overall filtering mechanism is designed to be very generic.  All the filters work the same, and can apply any functions in the toolbox, to any field, with these two special cases:

- The jurisdictionalFilter does not do any logging. Given that most items are for only one or maybe a couple juridictions out of hundreds, almost all the jurisdictionalFilters do filter, and that's normal, so not worth logging. Obviously its good to do jurisdictionalFiltering first!  The other three filters all do logging, so we can get an explanation of why data initially intended for a jurisdiction did not actually go there.
- The qualityFilter can be reversed for any receiver.  This is a simple way for Organizations to set up a receiver bucket that gets all the low quality data.  See example below.

### A Practical Example

```
name: "nj-doh"
filters:
  - topic: "covid-19"
    jurisdictionalFilter: [ "orEquals(ordering_facility_state, NJ, patient_state, NJ)" ]
    qualityFilter:  [ "hasAtLeastOneOf(order_test_date,specimen_collection_date)" , etc, etc]
receivers:
- name: "elr"
  routingFilters:  [ "doesNotMatch(sender_id, FlakyBakyTestsInc)" ]
- name: "flaky-baky"
  routingFilters:  [ "matches(sender_id, FlakyBakyTestsInc)" ]
- name: "secondary"
  reverseTheQualityFilter: true
- name: "test"
  processingModeFilter: [ "matches(processing_mode_code, T)" ]
```

#### Explanation

- **jurisdictionalFilter**.   As a safety feature, the Global Default jurisdictionalFilter for topic covid-19 is 'allowNone()'.  That is, you *must* override it, for an organization's receivers to get data, so don't forget!   Most of our organizations are geographic entitites, so it usually makes sense to only set the juridictionalFilter at the Organization level, not the receiver level.

- **qualityFilter**.   Covid-19 has a complex and carefully designed default qualityFilter.  (See default in ReportStreamFilter.kt).  However, an individual Organization can override the default if it requires a more or less stringent qualityFilter.  Its probably wise to do this once for the whole Organization.  This makes it easy to use the reverseTheQualityFilter=true flag, if that state wants to specify a `secondary` receiver for lower quality data.  And of course an individual Receiver can add further stringency to the Organization level qualityFilter, if needed.  However, a Receiver cannot ever override an Organization filter.

- **routingFilter**.  This is meant for general purpose usage.  The default for topic covid-19 is `allowAll()`, that is, it does no filtering.  Individual Organizations and/or Receivers can override this and apply more stringent rules as desired.  The original use case for routingFilter was when an Organization wants to disallow data from any one particular sender_id, as in the example above.

- **processingModeFilter**.  The default for covid-19 is `doesNotMatch(processing_mode_code, T, D)`.  Generally you should *never* override this, unless you have a receiver that specifically wants test data, as in the `test` receiver in the example.  The default processingModeFilter is a valuable check, and its what allows our senders to safely end junk/test/training PII data to our Production system, knowing it won't be forwarded to receivers.  That's why it gets it own special filter type.

A few subtleties:

- The `elr`, `flaky-baky` and `secondary` receivers will never get Test or Debug data, because they don't override the GlobalDefault processingModeFilter, which is always lurking in the background, to prevent that data from leaking through.
- The `elr` `flaky-baky` and `test` receivers will only get "good" quality data, as determined by the Organization-level qualityFilter.
- The `secondary` receiver will only get data that fails the qualityFilter.  It'll get all failed data, regardless of whether its for FlakyBakyTestsInc, or not.   
- The `test` filter will get all the Training/Test data that passes the qualityFilter for the jurisdiction.


### Errors

Note that filtering is generally _not_ an error.   If you think Item X should have gone to Receiver Y, and it didn't, then you need to change the filtering rules to make that happen. Or vice-versa!  We spend our lives trying to find that sweet spot between too much and too little, eh?

### Looking to the Future

1.  You can, if you wish, implement a fifth filter type, as needed.   However four is already a lot and getting confusing.
2. we need to move the default filters out of being hardcoded, into a global setting.  This is important for testing - we need to be able to modify it per-environment to adequately test. (Ticket 1329)
3.  Maybe implement sender filters.  The above is only for receiver filters and org filters.   Weirdly, since Senders have Organizations too, you could set filters for a Sender Organization, but it would just be ignored.
4. (Ticket 3309) : If desired change the yaml language for individual receivers from:

```
- jurisdictionalFilter:  [ "foo()" ]
- qualityFilter: [ "bar()" ]
- routingFilter:  [ "baz()" ]
```

To match the syntax in use for Organizations:

```
  - filters:
    jurisdictionalFilter:  [ "foo()" ]
    qualityFilter: [ "bar()" ]
    routingFilter:  [ "baz()" ]
```
(Note one complication in doing this is the `topic` is not needed for receivers but is for Organizations)