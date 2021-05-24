# EmailService proposal

## Background
It has been recognized that notifications (in particular via email) would be a desired feature to add to ReportStream.  Toward, this end - we have secured the use of SendGrid to manage the email process and provide a well-known source of emails.  But this environment is not sufficient to address all the needs.  While SendGrid can maintain templates and send emails - it doesn't have the necessary access to ReportStream data (which might lead to duplication of data) as well, it lacks any scheduler to send messages on a periodic basis.  For these reasons - it is proposed to build an EmailService.

## Goals

The EmailService should:

1. Respond to known and periodic events to send templated emails
1. Maintain a base of templates that can be updated as needs change
1. Maintain a schedule of emails to be sent
1. Have parameters that can be filled with the template based on
    - most recently sent emails
    - organization and organization participants
    - rellevant dates

## Proposal

The EmailService will run as a HTTP triggered service presenting a REST-ful API to manage EmailSchedules.  It will have capabilities to LIST, CREATE, DELETE, and UPDATE EmailSchedules.  When an EmailSchedule is created, the EmailServiceEngine a time-trigger based Azure function will wake up and based on the 'schedule' element within the supplied message for building the desired email will notifying SendGrid.  

The email service will store its relevant data within the Postgres database, with the exception of templates which will be maintained on the SendGrid site (with references to them in Postgres).

The EmailService will take in a Schedule element that consists of a body similar to:
``` json
{
    "template": "template.name", // the name of the template
    "schedule": "chron string for scheduling", // optional, no schedule means send immediate
    "organization" : "optional organization to send to", // ex. DHpima_az_doh, DHmt_doh, etc.
    "parameters": {
        "param1": "value for param1",
        ...
    }
}
```

See [openapi.emailservice.yml](./openapi.emailservice.yml) for a detailed specification

### EmailServiceEngine
The EmailServiceEngine is the portion of the system that will periodically awaken and dispatch to SendGrid based on:

- Retrieve a list of templates that need to be sent
- For each template - it will fetch (and cache) the listing of organizations to send this to
- For each organization, it will fetch the listing of emails (users) to send to
- emails will have a claim (opt-out) that lists the types of templates that they wish to opt out of
- organizations & emails will be cached
- Once obtained - per organization, the template/emails will be dispatched to SendGrid

The EmailService will record all of its actions to a logging table.

The EmailService will require an HttpHeader Authentication with a valid JWT token.

### OptOut
With each email sent, the message will allow for a link to unsubscribe and/or manage optout -- The link will go to a page that will allow for setting/changing the types of emails that the user wants to optout of...

projected types are
- Daily / Weekly
- Marketing
- Alerts

### Special parameter values
Special parameters are included automatically with each call to sendgrid

| Parameter | Description |
| --------  | ------------|
|$reports_since_last| retrieves an array of reports sent since the last email |
|$today | Java Date representing the current date/time |
|$org | The organization being referenced |
|$user | The username being referenced |

*Note: more to be added*

## Examples

### List all active EmailSchedules
#### Request
```http
GET /email-service/schedule HTTP/1.1
```
#### Response
```json
HTTP/1.1 200 OK

[
    {
        "id": "2130-9991",
        "template": "daily.tmpl",
        "schedule": "0 0 0 12", 
        "parameters": {
            "reports": "$reports_since_last",
            "today": "$today",
            "organization": "$org"
        }
    },
    {
        "id": "2130-9991",
        "template": "weekly.tmpl",
        "schedule": "0 0 0 12", 
        "organization": [ "DHpima_az_doh" ],
        "except": ["joe.jones@az.pima.gov"],
        "parameters": {
            "reports": "$reports_since_last",
            "organization": "$org"
        }
    }
]
```
#### Notes
In this case, we have 2 email schedules, the first uses the template "daily.tmpl" and is scheduled to fire daily at noon, with paramters for reports, today, and organization.  The second, uses a template "weekly.tmpl" and goes to everyone in the pima_az_doh organization (except for joe.jones) once a week on friday at noon.

### Create an EmailSchedule
#### Request
```json
POST /email-service/schedule HTTP/1.1

{
    "template": "daily.tmpl",
    "schedule": "0 0 0 12", 
    "organization": ["DHmt_doh"],
    "parameters": {
        "reports": "$reports_since_last",
        "today": "$today",
        "organization": "$org"
    }
}
```
#### Response
```json
HTTP/1.1 200 OK

{
    "id": "2130-9991"
}
```
#### Notes
In this case, we created the emailschedule to send the daily.tmpl to mt_doh daily at noon.

### Delete an EmailSchedule
#### Request
```http
DELETE /email-service/schedule/2130-9991
```
#### Response
```http
HTTP/1.1 200 OK
```
#### Notes
Successfully deleted 2130-9991.  Deleting a non-existant EmailSchedule will not result in an error.

### Replace an EmailSchedule
#### Request
```json
PUT /email-service/schedule/2130-9991

{
    "id": "2130-9991",
    "template": "daily2.tmpl",
    "schedule": "0 0 0 17", 
    "parameters": {
        "reports": "$reports_since_last",
        "today": "$today",
        "organization": "$org"
    }
}
```
#### Response
```http
HTTP/1.1 200 OK
```
#### Notes
Replaces the old 2130-9991 email schedule with something that sends the daily2.tmpl to everyone at 5PM.  Returns a ```HTTP/1.1 404 NOT_FOUND``` if the EmailSchedule doesn't exist

### Update an EmailSchedule
#### Request
```http
POST /email-service/schedule/2130-9991
{
    "template": "daily2.tmpl",
    "schedule": "0 0 0 17"
}
```
#### Response
```json
HTTP/1.2 200 OK
```
#### Notes
An alternate way to update the old 2130-9991 email schedule with just the elements that changed.  Returns a ```HTTP/1.1 404 NOT_FOUND``` if the EmailSchedule doesn't exist