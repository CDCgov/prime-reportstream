# A proposal for naming our customer-facing REST Endpoints

# Requirements:

1. Must use `waters`.
2. Must continue to support existing endpoints until fully removed. (`POST api/reports`, `POST api/waters`, `api/history/report`)
3. Must work for both single _Organization_ users and _Admin_ users.
4. Remember that an Organization can have many receivers and/or senders.   However, in practice, senders and receivers are separate organizations, except for `ignore`.  Requirement is: Must handle queries from both Receivers and Senders and Organizations that are both.
5. Must handle queries for _metadata_ (aka history/lineage) and for _data downloads_.
6. For metadata, must handle queries for Collections and Singletons.
7. For metadata, must handle queries based on UUID `id` (aka `reportId`) and/or integer `submissionId` (aka `actionId`)
Note:   At first we only supported `reportId` queries, but that doesn't handle the case of a failed submission. 
So we now also allow queries based on `submissionId`.
8. Must handle future queries for history of single items.  (No single item download)
9. Must include API versioning somehow.

# Proposed solution

## Quick summary

The waters endpoint will support these resources:

**Organization Resource**

| Endpoint                           | Brief Description                         | Old Endpoint                             | Priority |
|------------------------------------|-------------------------------------------|------------------------------------------|----------|
| GET api/waters/org/{o}/submissions | get list of submissions by a sending org  | GET api/history/{o}/submissions          | NOW      |
| GET api/waters/org/{o}/deliveries  | get list of deliveries to a receiving org | GET /api/history/report                  | 3        |
| GET api/waters/org/{o}/settings    | _Future_:  migrate settings queries       | GET api/settings/organizations/{orgName} | 4        |

**Report Resource**

| Endpoint                          | Brief Description                  | Old Endpoint                   | Priority |
|-----------------------------------|------------------------------------|--------------------------------|----------|
| POST api/waters/report            | to submit a payload                | POST api/waters                | 2        |
| GET api/waters/report/{r}/file    | to retrieve a data file.           | GET /api/history/report/{r}    | 3        |
| GET api/waters/report/{r}/history | to retrieve history about one file | GET api/history/{o}/report/{r} | NOW      |
| GET api/waters/report/{actionId}/history | (same!)                            | GET api/history/{o}/submissions/{actionId} | NOW      |

**Item Resource**

| Endpoint                        | Brief Description                  | Old Endpoint |
|---------------------------------|------------------------------------|--------------|
| GET api/waters/item/{i}/history | to retrieve the history of one item | n/a          | 

## The Details

## Organization Resource

### GET submissions by an Organization

For a single organization, get a list of its submissions to ReportStream

`GET api/waters/org/{orgName}/submissions`

If not an admin, this is an error if the JWT claim does not match orgName.
This is also an error if {orgName} has no senders in settings.  Otherwise, retrieve collection of all submission made by all senders
in that org.

### GET deliveries to an Organization

Get a list of deliveries made to a single organization.

`GET api/waters/org/{orgName}/deliveries`

If not an admin, this is an error if the JWT claim does not match orgName.
This is also an error if {orgName} has no receivers.  Otherwise, retrieve collection of all reports sent
(or meant to be sent) to that org.

### GET other settings for an organization

Placeholder for getting other settings in the future

## Report Resource

### POST a report

This is how to submit a payload:

`POST api/waters`  will be replaced with  `POST api/waters/report`

`POST api/reports` (active, but deprecated)

We consider the `api/reports` endpoint deprecated, but is still in widespread use at this time.

### GET a report's data file

Get an actual data file

`GET api/waters/report/{id}/file`

{id} must be a Report UUID.  This is an error if the JWT claim does not "own" the file.  As an admin, this should allow getting
any file at any step in the pipeline, for debugging.

History Tracking:  
_All_ such retreivals should be logged as 'actions'.  However, the receiver request is treated differently internally: this would count as a 'delivery' in the lineage.)
There is a sender-files function implemented in SenderFilersFunction.kt whose functionality overlaps this endpoint.

### GET a report's history

As a sender, get lineage/history of a singleton submission; As a receiver, get a lineage/history singleton delivery metdata 

`GET api/waters/report/{id}/history`
`GET api/waters/report/{submissionId}/history`

Note: we can figure out if this is a descendant or ancestor query (or an error) based on the id submitted.
Also, no need to specify the `org` - we can confirm that from the JWT claim.
It is an error for an organization user to call this on a report they do not 'own'.

The `{id}` is the report_id UUID primary key to the report_file table in the database.  
The `{submissionId}` is the action_id integer in the action table in the database.

## Item Resource

`GET api/waters/item/{trackingId}/history`

Its important to be able to search for an item without knowing what report its in.
No item-level queries exist yet, so these are future features not built yet.
It is an error if the trackingId is now 'owned' by the sender or receiver

## Other Important API Topics
### Organization users vs Admin users

Specified by the auth, as a jwt claim.  Does not appear in the URL at all.  If an Organization user, then the `{orgName}` in the URL must match the user's organization exactly.

### Versioning

Do this in a header.  eg, `curl -H "version:1"`

With no version specified, use the 'current' version. Which might be the latest version or one behind the latest version.

### Auth 

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
GET /api/history/report/{reportId}
GET /api/history/report/{reportId}/facilities
```
These are how our new Submission History API endpoints are named right now:
```
GET api/history/{orgName}/submissions
GET api/history/{orgName}/report/<reportID>
GET api/history/{orgName}/submissions/<actionID>
```

These are how the Settings API works:
```
GET api/settings/organizations/{orgName}
GET api/settings/organizations/{orgName}/senders/{senderName}
GET api/settings/organizations/{orgName}/receivers/{receiverName}
```
