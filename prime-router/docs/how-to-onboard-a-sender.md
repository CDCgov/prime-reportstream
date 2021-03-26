# How to Onboard a new Organization to Send Data

## Welcome

Our goal is to allow any organization to send information to us via our RESTful API, that we can then
translate and route to different receivers based on the jurisdictional filters we have.

It is important to note that ReportStream is in production now, and if care is not taken when posting
information, it can end up being routed to receivers. Therefore, do ***NOT*** post any information into 
production unless you are absolutely sure it should be going there.

As a simple running example, we will pretend we're creating a sender for Yoyodyne Propulsion, **YDP**.

## Steps

### Get Ready

Create a new branch to store your work on the new sender.

### Set up a New Organization

In your `organization-local.yml` file create a new organization. Senders typically are at the top of the 
file.

It should look something like this:

```yaml
- name: ydp
  description: Yoyodyne Propulsion Laboratories, the Future Starts Tomorrow!
  jurisdiction: FEDERAL
  senders:
    - name: default
      organizationName: ydp
      topic: covid-19
      schemaName: ydp/ydp-covid-19
      format: CSV
```

A few things to note here: 

- The name of the organization must be unique. It cannot share name with a state or other sender
- The jurisdiction should be FEDERAL since they are sending into the CDC (*NOTE*: this may change in the future)
- The organizationName under `senders` must match the name of the org above
- The format here is `CSV`, though it is possible it could another format, such as `HL7`
- Canonically, the schema name should be prefaced with the organization name

### Set up a New Schema

Once you've added the sender to the `organizations-local.yml` file you next need to create a schema file.

The schema describes the data coming in from the sender and maps back to the base schema, which for `covid-19`
is named `covid-19`. Schemas live in `metadata/schemas` so the one for Yoyodyne would be at
`metadata/schemas/YDP/ydp-covid-19.schema`

The header for a schema looks like this:

```yaml
name: ydp-covid-19
description: A COVID-19 schema for Yoyodyne Propulsion Lab
trackingElement: message_id
topic: covid-19
basedOn: covid-19
elements:
```

Given a simplistic CSV file that looks like this:
```csv
"Message ID","Patient First Name","Patient Last Name"
"1","Buckaroo","Banzai"
"2","Sidney","Zweibel"
```

The elements in the sender file would look like this:
```yaml
- name: message_id
  csvFields:
      - name: "Message ID"
  
- name: patient_first_name
  csvFields:
      - name: "Patient First Name"

- name: patient_last_name
  csvFields:
      - name: "Patient Last Name"
```


**NOTE** - A schema can either be `basedOn` the `covid-19` schema or `extends` it. If you choose `extends`, 
ReportStream will import ALL the elements from the parent schema into your schema which could cause errors.
For the purpose of senders, it is better to use `basedOn`

When a file is sent from Yoyodyne, ReportStream will match columns in the document based on the `csvFields.name`
property. The matching is done in a case-sensitive matter, though order isn't strictly enforced. We do try to 
match the order of the CSV file when creating schemas.

Each of the fields above, `message_id`, `patient_first_name`, `patient_last_name` are imported from the `covid-19` 
parent schema, which defines additional information about the fields, like datatype, documentation, etc



## Testing

## Conclusion