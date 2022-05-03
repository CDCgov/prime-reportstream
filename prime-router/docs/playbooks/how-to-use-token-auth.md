# How To Use FHIR Style Token Authentication and the Waters API.

The Waters API, the primary secure entrypoint to ReportStream, is named in memory of Dr. Michael Waters.

This playbook is a set of commandline scripts meant to exercise and demonstrate the 3 steps in FHIR style authentication.

A couple notes before we get started:

- Note: ReportStream has a new 'TokenSigningSecret' as part of this.   Locally, it is created for you by ./vault/config/init.sh upon container start.  There is a bug at the moment where you might have to restart your container the first time this value is created.

- Note:  Because organizations.yml will _overwrite_ your wonderful keys you uploaded in step 2, you will have to rerun that step every time you stop/start your docker and that file has changed.  Need to fix this pain point.  I didn't want to put keys into that file, even bogus ones.

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
./prime sender addkey --public-key ./my-es-public-key.pem  --scope waters.default.report --name waters.default 
./prime sender addkey --public-key ./my-es-public-key.pem  --scope waters.default.report --name waters.default --doit
./prime sender get --name waters.default
```

#### Just for fun, add the key to another sender.  In real life, you'd use a different public key.
```
./prime sender addkey --public-key ./my-es-public-key.pem  --scope ignore.ignore-waters.report --name ignore.ignore-waters
./prime sender addkey --public-key ./my-es-public-key.pem  --scope ignore.ignore-waters.report --name ignore.ignore-waters --doit
./prime sender get --name ignore.ignore-waters
```
**STEP 2.** The SENDER (a server, not a human) requests a token

The actual call would be a call to the REST API endpoint, which is hidden in this CLI call.

(Note: this is the step that requires the TokenSigningSecret, which should get generated automatically.)

Note:  No, we are NOT intending that our customers use the prime CLI to do this!   This is just a convenience, for us (only) to test and demo.  And yes, more work is needed to provide documentation and helper tools for our senders.

```
./prime sender reqtoken --private-key my-es-keypair.pem --scope waters.default.report --name waters.default
```

If it works, you should get something like this back:

```
{"access_token":"<long string of jwt glop>","token_type":"bearer","expires_in":300,"expires_at_seconds":1625260982,"scope":"waters.default.report"}
```

This token is only valid for 5 minutes.

**STEP 3**  The SENDER (again, a server, not a human) uses that token to send a report:

Grab just the `access_token` value you got back from step 3, and use it here for the bearer token:

```
curl -H "authorization:bearer ???" -H "client:waters"  -H "content-type:text/csv" --data-binary "@./junk/waters.csv" "http://localhost:7071/api/waters"
```

**Bonus Steps**

To avoid cutting and pasting by hand, this glorious string of unixy gibberish will request a signed 5minute access token (Same as STEP 2 above), and then paste it into an environment variable called `$TOK`:

```
export TOK=$(./prime sender reqtoken --private-key my-es-keypair.pem --scope simple_report.default.report --name simple_report.default |  grep access_token | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")
```

Which can then be used very simply like this (Same as STEP 3 above):

```
curl -H "authorization:bearer $TOK" -H "client:waters"  -H "content-type:text/csv" --data-binary "@./junk/waters.csv" "http://localhost:7071/api/waters"
```
