import jwt
import time
import requests
import uuid
import pprint

"""
============================
CREATE THE OKTA API JWT
============================
"""

"""
okta_private_key and client_id is generated within Okta by an RS onboarding person and emailed
to the client. See https://developer.okta.com/docs/guides/implement-oauth-for-okta-serviceapp/main

See this documentation to understand how to setup an authorization server:
https://developer.okta.com/docs/guides/customize-authz-server/main/#create-scopes
"""
authorization_server_id = "" # set your authorization server id here
client_id = "" # set you application user's client it
okta_private_key = """""" # set your application user's private key in PEM format
okta_kid = "" # set your application user's Key ID string
requested_scope = "sender" # update requested scope to desired one
dpop_private_key = """""" # Set DPoP private key in PEM format
dpop_public_key_json = {} # set DPoP public key in JWK json format

# "jti": "AT.MbfIc2QBgzpRCb_C3ClbH-E888fnSbdKJE1NNFUrwVg",
encoded_jwt = jwt.encode(
    {
        "ver": 1,
        "aud": f"https://reportstream.oktapreview.com/oauth2/{authorization_server_id}/v1/token",
        "iat": time.time(),
        "exp": time.time() + 1000,
        "cid": client_id,
        "iss": client_id,
        "sub": client_id,
        "scp": [
            requested_scope
        ],
        "auth_time": 1000
    },
    okta_private_key,
    algorithm='RS256',
    headers={
        "alg": "RS256",
        "kid": okta_kid
    }
)
print("The encoded JWT")
print(encoded_jwt)
print("="*15)

"""
==============================================================
CREATE THE DPoP JWT (ONLY IF THE APP HAS DPoP ENABLED IN OKTA)
==============================================================
"""


encoded_dpop_jwt = jwt.encode(
    {
        "htm": "POST",
        "htu": f"https://reportstream.oktapreview.com/oauth2/{authorization_server_id}/v1/token",
        "iat": time.time(),
    },
    dpop_private_key,
    algorithm='RS256',
    headers={
        "typ": "dpop+jwt",
        "alg": "RS256",
        "jwk": dpop_public_key_json
    }
)
print("The encoded DPoP JWT")
print(encoded_dpop_jwt)
print("="*15)

"""
============================
MAKE THE OKTA TOKEN REQUEST
============================
"""
headers = {
    "Accept": "application/json",
    "Content-Type": "application/x-www-form-urlencoded",
    "DPoP": encoded_dpop_jwt
}
payload = {
    "grant_type": "client_credentials",
    "scope": requested_scope,
    "client_assertion_type": "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
    "client_assertion": encoded_jwt,
    "client_id": client_id
}
r = requests.post(
    f"https://reportstream.oktapreview.com/oauth2/{authorization_server_id}/v1/token",
    params=payload,
    headers=headers
)

"""
============================
UPDATE NONCE IN DPoP JWT
============================
"""
encoded_dpop_jwt_with_nonce = jwt.encode(
    {
        "htm": "POST",
        "htu": f"https://reportstream.oktapreview.com/oauth2/{authorization_server_id}/v1/token",
        "iat": time.time(),
        "nonce": r.headers["dpop-nonce"],
        "jti": str(uuid.uuid4())
    },
    dpop_private_key,
    algorithm='RS256',
    headers={
        "typ": "dpop+jwt",
        "alg": "RS256",
        "jwk": dpop_public_key_json
    }
)
print("The encoded DPoP JWT")
print(encoded_dpop_jwt)
print("="*15)

"""
============================================
MAKE THE OKTA TOKEN REQUEST WITH UPDATE DPoP
============================================
"""
headers = {
    "Accept": "application/json",
    "Content-Type": "application/x-www-form-urlencoded",
    "DPoP": encoded_dpop_jwt_with_nonce
}
payload = {
    "grant_type": "client_credentials",
    "scope": requested_scope,
    "client_assertion_type": "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
    "client_assertion": encoded_jwt,
    "client_id": client_id
}
r = requests.post(
    "https://reportstream.oktapreview.com/oauth2/ausekaai7gUuUtHda1d7/v1/token",
    params=payload,
    headers=headers
)
print("request response: ", r.text)
print(r)
encoded_token = r.json()["access_token"]
print("ENCODED TOKEN: ", encoded_token)
decoded_token = jwt.decode(encoded_token, options={"verify_signature": False})
print("\n\n\nFINAL DECODED BEARER TOKEN:")
print("===========================")
pprint.pprint(decoded_token)
