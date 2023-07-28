# Quality Filters

This proposal discusses the idea of each Receiving having one or more data QualityFilters, just like they now have JurisdictionalFilters.

These would be used to filter out data that does not meet the state's standards.

To support this, I've implemented two new filters:
### hasValidDataFor(a,b,c)
- If an Item has valid data for *all* the columns a,b,c, the row is selected.
- If any of the columns a,b,c does not exist, nothing passes thru the filter.

### hasAtLeastOneOf(a,b,c)
- If an Item has valid data for *any one of* the columns a,b,c, the row is selected.
- If none of the columns a,b,c exist, nothing passes thru the filter.

### Default Filter

A key idea is the Default Filter - if a state does not specify an alternative, we do some basic filtering on their behalf.

Based on some analysis (below), we propose the following Default Quality Filter for topic 'covid-19':
 ```
        // valid human and valid test
         "hasValidDataFor(" +
             "message_id," +
             "equipment_model_name," +
             "specimen_type," +
             "test_result," +
             "patient_last_name," +
             "patient_first_name," +
             "patient_dob" +
         ")",
         // has valid location (for contact tracing)
         "hasAtLeastOneOf(patient_street,patient_zip_code)",
         // has valid date (for relevance/urgency)
         "hasAtLeastOneOf(order_test_date,specimen_collection_date_time,test_result_date)",
         // able to conduct contact tracing
         "hasAtLeastOneOf(patient_phone_number,patient_email)"
 ```

Important Note:  Any state can override the default with a hand-crafted quality check, or with no quality check
There is a filter called allowAll that allows all the data thru the quality check step.

Examples:
```
jurisdictionalFilter:
- "matches(ordering_facility_state, OH)"
qualityFilter:
- "allowAll"    # No quality filtering
```

```
jurisdictionalFilter:
- "matches(ordering_facility_state, OH)"
qualityFilter:
- "hasValidDataFor(patient_id)"    # Only filter out data with missing patient_id.  All other data stays.
```

## ANALYSIS

The purpose of the exercise is to figure out the bare minimum set of required covid-19 fields.
in order to send that data forward to any state.

I started by looking only at the fields that currently exist in all three schemas:  SimpleReport, Strac, and Waters.
There are just under 30 fields that appear in all three(!), so that makes a much easier starting point.

Note: If you want to require a field that is, say, not in SimpleReport schema, that's the same as eliminating 
all the data from SimpleReport!  We'd need to tell SimpleReport we can't currently send any of their data anywhere, 
until they add that field.

###  Here are the Fields I suggest we require be complete with valid data, in order to send to any state:

#### Basic Info and IDs on the test.  All these must appear for the data to go to a state
  - name: message_id
  - name: equipment_model_name
  - name: specimen_type
  - name: test_result
  - name: testing_lab_clia
  - name: patient_last_name
  - name: patient_first_name
  - name: patient_id

#### Date Fields - Must supply data in at least one of these:
  - name: order_test_date
  - name: specimen_collection_date_time
  - name: test_result_date

## Fields in all three schemas, that I suggest we do NOT require:
This doesn't mean they aren't important - 
just that, if missing, we won't use that as a reason to refuse to send a test result to any state

  - name: patient_zip_code ??? should we require this?
  - name: patient_dob  ??? Should we require this?
  - name: ordering_provider_zip_code  ??? Should we require this?
  - name: ordering_provider_state  Note: CAN BE EMPTY IN SimpleReport
  - name: ordering_provider_street  *Note:* CAN BE EMPTY IN SimpleReport
  - name: ordering_provider_street2 *Note:* Not crucial
  - name: patient_city  *Note:* CAN BE EMPTY IN SimpleReport
  - name: patient_county
  - name: patient_state   *Note:* data won't go anywhere without this anyway, so no reason to have it in the above list
  - name: patient_email  *Note:* CAN BE EMPTY IN SimpleReport
  - name: patient_ethnicity
  - name: patient_gender
  - name: patient_phone_number
  - name: patient_race
  - name: patient_street
  - name: patient_street2   *Note:* NOT CRUCIAL
