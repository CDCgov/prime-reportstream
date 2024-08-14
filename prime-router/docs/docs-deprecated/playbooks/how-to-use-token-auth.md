# How To Use FHIR Style Token Authentication and the Waters API.

The Waters API, the primary secure entrypoint to ReportStream, is named in memory of Dr. Michael Waters.

This playbook is a set of commandline scripts meant to exercise and demonstrate the 3 steps in FHIR style authentication.

### Setup Notes

A few  notes before we get started:

- If you re-load settings (from  organizations.yml), you'll overwrite any keys uploaded.

- All the steps here assume a fake sending organization/client-id, called `healthy-labs`, with its sender fullname as `health-labs.default`
- There is no actual `healthy-labs` in the system.  If you want a real dummy sender, try `waters` or `ignore`.

- This example uses kotlin code in the ReportStream CLI to sign the token.
There is also an example written in python, in [../../examples/generate-jwt-python](../../examples/generate-jwt-python)

- Scopes are of the format `<orgname>.<sendername>.<resource-or-role>`.   The scope `health-labs.*.report`, used below, gives access to all CRUD on all reports for any sender within the healthy-labs organization.



OK, here we go...

**STEP 1a**. The SENDER to ReportStream (a human) generates a new keypair

**EC**
```
openssl ecparam -genkey -name secp384r1 -noout -out my-es-keypair.pem
openssl ec -in my-es-keypair.pem -pubout -out  my-es-public-key.pem
```
**RSA**
```
openssl genrsa -out my-rsa-keypair.pem 2048
openssl rsa -in my-rsa-keypair.pem -outform PEM -pubout -out my-rsa-public-key.pem
```

**STEP 1b.**  The REPORTSTREAM ONBOARDING MANAGER (a human) stores the public key in ReportStream, based on trust relationship

First, make sure you have Reportstream running.   These calls below are what the person on the Reportstream side would run, to store the public key:

#### The first call (without --doit) just tests that it works.  Then use --doit to execute.
```
./prime organization addkey --public-key ./my-rsa-public-key.pem  --scope "healthy-labs.*.report" --orgName healthy-labs --kid healthy-labs.unique-value
./prime organization addkey --public-key ./my-rsa-public-key.pem  --scope "healthy-labs.*.report" --orgName healthy-labs --kid healthy-labs.unique-value --doit
./prime organization get --name healthy-labs
```

**STEP 2.** The SENDER (a server, not a human) requests a token

The actual call would be a call to the REST API endpoint, which is hidden in this CLI call.

(Note: this is the step that requires the TokenSigningSecret, which should get generated automatically.)

Note:  No, we are NOT intending that our customers use the prime CLI to do this!   This is just a convenience, for us (only) to test and demo.  And yes, more work is needed to provide documentation and helper tools for our senders.

```
./prime organization reqtoken --private-key my-rsa-keypair.pem --scope "healthy-labs.*.report" --name healthy-labs --kid healthy-labs.unique-value
```

If it works, you should get something like this back:

```
{"access_token":"<long string of jwt glop>","token_type":"bearer","expires_in":300,"expires_at_seconds":1625260982,"scope":"healthy-labs.*.report"}
```

This token is only valid for 5 minutes.

**STEP 3**  The SENDER (again, a server, not a human) uses that token to send a report:

Grab just the `access_token` value you got back from step 3, and use it here for the bearer token:

```
curl -H "authorization:bearer ???" -H "client:healthy-labs"  -H "content-type:text/csv" --data-binary "@./junk/healthy-labs.csv" "http://localhost:7071/api/waters"
```

**Bonus Steps**

To avoid cutting and pasting by hand, this glorious string of unixy gibberish will request a signed 5minute access token (Same as STEP 2 above), and then paste it into an environment variable called `$TOK`:

```
export TOK=$(./prime organization reqtoken --private-key my-rsa-keypair.pem --scope "healthy-labs.*.report" --name healthy-labs --kid healthy-labs.unique-value |  grep access_token | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")
```

Which can then be used very simply like this (Same as STEP 3 above):

```
curl -H "authorization:bearer $TOK" -H "client:healthy-labs"  -H "content-type:text/csv" --data-binary "@./junk/healthy-labs.csv" "http://localhost:7071/api/waters"
```
