## Example ReportStream client/sender auth code, in python

This directory contains a python example of how to connect to ReportStream as a sender.

#### Prerequisites
- Python 3
- PyJWT module (`python3 -m pip install PyJWT`)
- cryptography moddule (`python3 -m pip install cryptography`)

### Steps to Authenticate with ReportStream, using Tokens

Authentication is done in three steps.   Details are found in the ReportStream-Programmers-Guide.docx, in https://github.com/CDCgov/prime-reportstream/tree/master/prime-router/docs, but here is a quick outline:

#### Step 1:  Prior to submission, send your public key to ReportStream

- Prior to connecting to the endpoint, you’ll create a public/private keypair, and send the public key to ReportStream.
- ReportStream will give you back a unique client-id for you to use.
- You only need to do this step once, not every time you submit. 
- This example does not include this step -- you'll need to contact ReportStream directly for this step.

#### Step 2:  At the Time of Submission, generate a signed JWT using your private key

- The `generate-jwt.py` program here in this folder is an example of how to do that.

#### Step 3:  Send the signed JWT to ReportStream, to get a temporary bearer token

- When run the, `generate-jwt.py` program prints out an example of what this looks like.

#### Step 4:  Use the temporary bearer token to send data to ReportStream

- When run the, `generate-jwt.py` program prints out an example of what this looks like.

### How to run this python example

#### Setup

1. Working in this folder, follow the setup steps in  https://auth0.com/blog/how-to-handle-jwt-in-python/ prior to running generate-jwt.py

2. In `generate-jwt.py`, make these two changes:
- Change `my_client_id` to be your unique client-id string, as given to you by the ReportStream team back in Step 1.
- Point the `my_rsa_keypair_file` to be the path to your generated rsa keypair pem file.

#### Run

Then run it:

```
python3 ./generate-jwt.py
```

### Further documentation

- Your main source of documentation should be the ReportStream-Programmers-Guide.docx, in https://github.com/CDCgov/prime-reportstream/tree/master/prime-router/docs.
- If python is not your cup of tea, then an example for how to generate a sender token, written in kotlin, can be found in function `generateSenderToken` in https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/src/main/kotlin/tokens/SenderUtils.kt
- Our implementation is based on these standard guidelines for the FHIR community:  http://hl7.org/fhir/uv/bulkdata/authorization/index.html
- Documentation meant mainly for ReportStream operations personnel can be found here:  https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/playbooks/how-to-use-token-auth.md
- Documentation on our token auth implementation (meant for ReportStream coders, but might be of general interest) is found here:  https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/proposals/0001-authentication.md



