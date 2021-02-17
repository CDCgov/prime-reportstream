# How to Onboard a new State or local jurisdiction to receive Hub data

## Welcome

Our goal is to onboard as many states and local jurisdictions as we can, to receive Hub data!
This is our internal documentation for how we _currently_ do that onboarding work.

As a simple running example, we'll pretend we're creating a new state, the State of Love and Trust, **LT**.  

## Steps

### Get ready

-  Create a new branch in git for your changes.

### Set up a new organization

Create a new organization for the State, (canonical style: `lt-phd`), in organizations.yml, which is used by your local commandline ./prime cli tool.  Follow the pattern of another existing organization.  Carefully set the initial jurisdiction-filter so that data is limited to that state.  (The jurisdiction: STATE and  stateCode: XY  fields should soon provide better enforcement of this)

The new organization must have at least one `receiver` defined, but the `receiver` does not need to have a transport defined - the download site can be used until an automated delivery mechanism is set up.

There are two fields that will be used as "keys" to further work, below.   The `-name` of the organization (eg, `lt-phd`) will be used as the Okta `group`, and the `translation.schemaName:` value (eg, `lt/lt-covid-19`) will be used as the schema name in the next step.

### Set up a new schema

- Canonical location style:   `metadata/schemas/LT/lt-covid-19.schema)
- Canonical name style: `name: lt-covid-19`
- Typically you can copy a basic/standard schema based on a previous state, and modify.

### Generate test data

Generate fake, or better, synthesized test data.

#### Generate some fake data for testing
```
./prime data --input-fake 100 --input-schema lt/lt-covid-19 --output myfile.csv --target-state LT
```

#### Synthesized data generation:
```
?
```

### Test and commit, and deploy to Test and maybe Prod

- Test locally using the above fake data.
- Once you've got the kinks out of the organizations.yml, carefully copy the info into organizations-test.yml, and make adjustments as needed.
- If you are ready, also carefully copy it inot organizations-prod.yml.  Especially in production, check the batch timing.  NOT every minute, eh?
- Create a PR for the change, review, and push to Test.  The review is a good chance for someone to doublecheck the filters.
- It should deploy to Test automagically once the PR is approved and merged into master.
- Test again in Test
- If needed, push to production following our procedures for doing that.

### Create access to the Download site

- Set up an Okta account for **LT**.  Be sure to include an internal Hub staffperson as a user, so they can test connectivity.  
- If you are testing in Test, obvioulys you'll need to set up access to that download site.

### Validation in Prod

- Work with the customer to confirm their rules for validation in Prod.   PII vs no PII.  Synthesized data vs real data (yes, it appears many PHDs test using real data.)
- At this point you should be able to send data through to the customer, and they can validate.
- You may want to set the Processing_mode_code field to 'D' or 'T' to represent Debugging or Training data.
- Customer from **LT** should be able to go to the download site and pull down data.

### Iterate on the schema with the customer

- If you change organization data, think about which of our 4 org files it needs to go into (sigh)
