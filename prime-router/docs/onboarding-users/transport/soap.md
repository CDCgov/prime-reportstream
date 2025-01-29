#### SOAP

#### Introduction
This is an introduction on how to configure a soap receiver for ReportStream. Note: SOAP is a very old and cumbersome protocol and we do not encourage receivers to use SOAP transport if any other option is available. There are currently only two STLTs using SOAP transport - PA and AR.

#### Assumptions
1. Superuser access to Azure to store credentials
2. General familiarity with creating receiver settings in ReportStream


#### Background
Simple Object Access Protocol(SOAP) is a network protocol for exchanging structured data between nodes. It uses XML format to transfer messages. The message in XML format contains three parts.

- Envelope: It specifies that the XML message is a SOAP message. A SOAP message can be defined as an XML document containing header and body encapsulated in the envelope. The fault is within the body of the message.
- Header: This part is not mandatory. But when it is present it can provide crucial information about the applications.
- Body: It contains the actual message that is being transmitted. Fault is contained within the body tags.
- Fault: This section contains the status of the application and also contains errors in the application. This section is also optional. It should not appear more than once in a SOAP message

Important syntax rules:

- A SOAP message MUST be encoded using XML
- A SOAP message MUST use the SOAP Envelope namespace
- A SOAP message must NOT contain a DTD reference
- A SOAP message must NOT contain XML Processing Instructions

SOAP has two versions that ReportStream Supports: SOAP v1.1 and SOAP v1.2: For ReportStream, PA uses SOAP v1.1 and AR uses SOAP v1.2. Both SOAP Version 1.1 and SOAP Version 1.2 are World Wide Web Consortium (W3C) standards. The main differences are summarized [here](https://www.w3.org/2003/06/soap11-soap12.html). The vast majority of differences in a given SOAP message structure are going to be the result of user defined XML elements and not differences in SOAP version as can be seen in the three examples below.

Example Skeleton SOAP Message:
```xml
<?xml version='1.0' encoding='UTF-8'?>
<rootnamespace:Envelope xmlns:rootnamespace="http://schemas.xmlsoap.org/soap/envelope/" xmlns:namespace="https://receiverdefinedurn">"
    <rootnamespace:Header/>
        <namespace:receiverdefinedelement1>
        </namespace:receiverdefinedelement1>
    <rootnamespace:Header/>
        <rootnamespace:Body>
         <namespace:receiverdefinedelement2>
         </namespace:receiverdefinedelement2>
        </rootnamespace:Body>
</rootnamespace:Envelope>
```

Example PA SOAP V1.1 Message:
```xml
<?xml version='1.0' encoding='UTF-8'?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:elr="https://receiverdefinedurn">
    <soapenv:Header/>
    <soapenv:Body>
        <elr:TestCredentials>
            <elr:UserName>$userName</elr:UserName>
            <elr:Password>$password</elr:Password>
            <elr:Timestamp>$timestamp</elr:Timestamp>
        </elr:TestCredentials>
        <elr:TestFile>FileContents</elr:TestFile>
    </soapenv:Body>
</soapenv:Envelope>

```

Example AR SOAP V1.2 Message:

```xml
<?xml version='1.0' encoding='UTF-8'?>
<soapenv:Envelope xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope" xmlns:elr="https://receiverdefinedurn">
    <soapenv:Header>
        <wsse:Security soapenv:mustUnderstand="1" xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
            <wsu:Timestamp wsu:Id="$timestampId" xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
                <wsu:Created>$timeCreated</wsu:Created>
                <wsu:Expires>$timeExpired</wsu:Expires>
            </wsu:Timestamp>
        </wsse:Security>
    </soapenv:Header>
    <soapenv:Body>
        <elr:TestSoap12Message>
            <elr:TestSoap12Payload>FileContents</elr:TestSoap12Payload>
        </elr:TestSoap12Message>
    </soapenv:Body>
</soapenv:Envelope>
```
Because of the wide variance in message structure between SOAP implementations you will need either an implementation guide from the receiver or a WSDL document that describes the required header and body data elements.

#### Steps
1. Create receiver settings
2. Determine needed header elements and  create new ones if necessary
3. Create body elements
4. Create new parameters for HTTP POST (optional)
5. Upload credentials to Azure

#### Step One - Create Receiver Settings

Create receiver settings in the same manner as you would for any other transport type. See [receiver.md](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/onboarding-users/receivers.md) document for more info.

SOAP Transport settings have the following unique parameters:

- endpoint: The URL endpoint to connect to (need to get from receiver)

- soapAction: The SOAP action to invoke (need to get from receiver)

- soapVersion: The SOAP version the receiver expects (can be left blank is using SOAP V1.1, is using SOAP V1.2 should be "SOAP12")

- credentialName: name of credential in Azure key vault if different than orgname--receivername (usually left blank)

- namespaces: list of namespaces used in headers and/or body (need to get from receiver)

- tlsKeystore: The name for the credential manager to get the JKS used in TLS/SSL if using certificate authentication.


#### Step Two: Determine needed header elements and create new ones if necessary

The required soap header elements will be documented in the receiver implementation guide or the receiver WSDL. SOAP header elements are hardcoded into the [Soap Serializer](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/src/main/kotlin/serializers/SoapSerializer.kt). If "SOAP12" is used as the soapVersion in the receiver transport settings the following headers will be used:

```xml
<soapenv:Header>
    <wsse:Security soapenv:mustUnderstand="1" xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
        <wsu:Timestamp wsu:Id="$timestampId" xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
            <wsu:Created>$timeCreated</wsu:Created>
            <wsu:Expires>$timeExpired</wsu:Expires>
        </wsu:Timestamp>
    </wsse:Security>
</soapenv:Header>
```
If the soapVersion is left null than there will be no elements in the header (the header will still exist with the appropriate namespace, but it will not contain any elements)

```xml
<soapenv:Header/>
```

If new header elements are needed the serialize function within the [Soap Serializer](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/src/main/kotlin/serializers/SoapSerializer.kt) will need to be modified.


#### Step Three: Create body elements

SOAP Body elements and structure need to be hardcoded into their own data class. See examples for [PA](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/src/main/kotlin/serializers/soapimpl/PAELRImpl.kt) and [AR](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/src/main/kotlin/serializers/soapimpl/Soap12Message.kt). The created data classes are then serialized into XML objects by function getXMLObjectForAction based on the soapAction parameter.

#### Step Four: Create new parameters for HTTP POST (optional)

SOAP V1.1 and SOAP V1.2 have different parameters for the headers required in the HTTP POST. For SOAP V1.1 the Content-type and SOAPAction are separate headers whereas for SOAP V1.2 SOAPAction is a parameter of the Content-Type header.

ex: SOAP V1.1
```xml
POST: https://somehostname.gov
Host: 127.0.0.1
Content-Type: text/xml; charset=utf-8 Content-Length: length
SOAPAction: "urn:UploadELRMessage"
```
ex: SOAP v1.2
```xml
POST: https://somehostname.gov
Host: 127.0.0.1
Content-Type: application/soap+xml; soapaction: "urm:UploadELRMessage"; charset=utf-8 Content-Length: length
```

SOAP V1.1 also has a content type of "text/xml" while SOAP V 1.2 has a content type of "application/soap+xml". The KTOR library does not contain a content-type of "application/soap+xml" so this is hardcoded into a header in our SOAPTransport.

#### Step Five: Create and upload credentials to Azure

ReportStream's SOAP transport currently supports Username/Password and Client Certificate Authentication (stored as a jks) options for authenticating with a receiver. These credentials are stored in the clientconfig keyvaults in Azure in a JSON format.

### Creating a credential

A credential in JSON format can be created by using the ./prime create-credential CLI command:

```yaml
create credential JSON or persist to store

  Use this command to assist with creating credentials secrets for the secret
  store.

  By default, this command will only output the JSON that should be stored in
  the secret store. This is useful for generating the JSON blob that should be
  stored in Azure's Key Vault.

  If you add the --persist option with a key, the command will store the secret
  in your local secrets store. Your environment must be configured properly to
  persist your secrets. See getting_started.md for some ideas on how you can do
  that for your environment.

Options for credential type 'UserPass':
  --user=<text>  SFTP username
  --pass=<text>  SFTP password

Options for credential type 'UserPem':
  --pem-user=<text>       Username to authenticate alongside the PEM
  --pem-file=<path>       Path to the PEM file
  --pem-file-pass=<text>  Password to decrypt the PEM
  --pem-user-pass=<text>  The password to use to login with the user if the
                          SFTP server is using partial auth

Options for credential type 'UserPpk':
  --ppk-user=<text>       Username to authenticate alongside the PPK
  --ppk-file=<path>       Path to the PPK file
  --ppk-file-pass=<text>  Password to decrypt the PPK (optional)
  --ppk-user-pass=<text>  The password to use to login with the user if the
                          SFTP server is using partial auth

Options for credential type 'UserJks':
  --jks-user=<text>           Username to authenticate alongside the JKS
  --jks-file=<path>           Path to the JKS file
  --jks-file-pass=<text>      the JKS passcode (optional)
  --jks-private-alias=<text>  the JKS alias that points to the ID certificate
  --jks-trust-alias=<text>    the JKS alias that points to a trust certificate

Options for credential type 'UserApiKey':
  --apikey-user=<text>  Username to authenticate alongside the API key
  --apikey=<text>       the API key

Options:
  --type=(UserPass|UserPem|UserPpk|UserJks|UserApiKey)
                    Type of credential to create
  --persist=<text>  credentialId to persist the secret under
  -h, --help        Show this message and exit
```

### Storing Credential in Azure

You will need an SU "super-user' account to complete this step. Login into Azure with a super user account and select the client keyvault for the appropriate environment. Select "secrets" from the right hand menu and then "generate/import" from the available options at the top of the screen. Inpute the desired secret name and then copy the credential in JSON format into the "secret value" field. Click the "create" button at the bottom of the screen to save the credential.