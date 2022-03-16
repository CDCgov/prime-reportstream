# A proposal for naming our customer-facing REST Endpoints

## Requirements:

1. Must use `waters`.
2. Must continue to support existing endpoints until fully removed. (`POST api/reports`, `POST api/waters`, `api/history/report`)
3. Must work for both single _Organization_ users and _Admin_ users.
4. Remember that an Organization can have many receivers and/or senders.   However, in practice, senders and receivers are separate organizations, except for `ignore`.  Requirement is: Must handle queries from both Receivers and Senders and Organizations that are both.
5. Must handle queries for _metadata_ (aka history/lineage) and for _data downloads_.
6. For metadata, must handle queries for Collections and Singletons.
7. For metadata, must handle queries based on UUID `id` (aka `reportId`) and/or integer `submissionId` (aka `actionId`)
8. Must handle future queries for history of single items, reportI and index, or trackingId.  (No single item download)
9. Must include API versioning somehow.

## Proposed solution

### Organization users vs Admin users

Specified by the auth.  Does not appear in the URL at all.  If an Organization user, then the `<org>` in the URL must match the user's organization exactly.

### How to submit a payload:

`POST api/waters`
`POST api/reports`

This matches the current POST endpoint, so no changes.  We consider the `reports` endpoint deprecated, but is still in widespread use at this time.

### Collection queries

As a sender, get a collection of submissions; As a receiver, get a collection of deliveries available to me

`GET api/waters/<org>/submissions`

This is an error if <org> has no senders.  Otherwise, retrieve collection for all senders with that org.

`GET api/waters/<org>/deliveries`

This is an error if <org> has no receivers.  Otherwise, retrieve collection for all receivers with that org.

### Singleton queries

As a sender, get lineage/history of a singleton submission; As a receiver, get a lineage/history singleton delivery metdata 

`GET api/waters/<org>/submissions/<id>`
`GET api/waters/<org>/submissions/<submissionId>`
`GET api/waters/<org>/deliveries/<id>`

The `<id>` is the report_id UUID primary key to the report_file table in the database.  Example:  `GET api/waters/<org>/submissions/1fcce643-5b5e-4408-9755-153754f9f779`.
The `<submissionId>` is the action_id integer in the action table in the database.  Example: `GET api/waters/<org>/submissions/4687281`

All These queries only get metadata, not actual data

### Get an actual data file

As a receiver, I want to download an actual data file

`GET api/waters/<org>/deliveries/file/<id>`

As an admin, get any file at any step in the pipeline, for debugging:

`GET api/waters/file/<id>`

(Note: the admin req is treated differently internally: this would not count as a 'delivery' in the lineage.)

### As a sender or receiver, I want history of what happened to a single item.

`GET api/waters/<org>/submissions/<id>/item/<trackingId>`
`GET api/waters/<org>/submissions/<submissionId>/item/<trackingId>`
`GET api/waters/<org>/deliveries/<id>/item/<trackingId>`

No item-level queries exist yet, so these are future features not built yet.

## Versioning

Do this in a header.  eg, `curl -H "version:1"`

With no version specified, use the 'current' version. Which might be the latest version or one behind the latest version.

## Auth 

Currently
`/api/reports` uses Azure `x-function-keys` auth.  The organization is specified using a header eg, `curl -H "client:simple_report"`
And `/api/waters` uses two-legged auth and okta auth.  The organization is specified using a header eg, `curl -H "client:simple_report"`, but should/could be done with auth info?


## "Old" endpoints:
For reference, here's the list of endpoints in the current system.

These are our current two submission endpoints
```
POST /api/reports
POST /api/waters
```
These are supported by our old/deprecated HistoryFunctions.kt:
```
GET /api/history/report
GET /api/history/report/{reportId}   This gets both metadata and data!
GET /api/history/report/{reportId}/facilities
```
These are how our new Submission History API endpoints are named right now:
```
GET api/history/<org>/submissions Get a summary list of submissions. paginated. GET api/history/<org>/submissions/<actionID> Get a single lineage-history-details.
GET api/history/<org>/report/<reportID> Get a single lineage-history-details.
GET api/history/<org>/submissions/<actionID>
```
