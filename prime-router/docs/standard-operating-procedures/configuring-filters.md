# Configuring Filters

## Frontend User Interface

The admin user interface at https://reportstream.cdc.gov/ allows a PRIME admin to
manage the settings of an organization, sender and/or receiver. Filters are configured as free text and the input text
must conform to the expected syntax.

## Command Line Interface

All filters for receivers and organizations can be created/updated/deleted via the command line.

1. create a .yml file containing the updated FHIRPath expressions. Ensure the file begins with “---”. Example:

```yaml
---
-   name: yoyodyne
    description: Yoyodyne Propulsion Laboratories, the Future Starts Tomorrow!
    jurisdiction: FEDERAL
    receivers:
        -   name: ELR
            externalName: yoyodyne ELR
            organizationName: yoyodyne
            topic: full-elr
            customerStatus: active
            jurisdictionalFilter: [ "(Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state.exists() and Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state = 'CA') ]
```

2. Use the following commands to load the information from the .yml files into the staging database. First obtain a
   login token for staging

`./prime login -–env staging`

Next update the staging DB

`./prime multiple-settings set –-env staging -–input <file-location>`
