loThe keystore for the program will contain many parts

- Sender certificate to sign the payload
- Receiver public certificate to encrypt the payload

1. Generate a private key
```
openssl genrsa -out cdcprime.key 2048
```

2. Generate a CSR for Entrust
```
openssl req -new -sha256 -key cdcprime.key -out cdcprime.csr
```

3. Download the .PEM file from Entrust
```
prime-cdc-gov-sender.pem
```

4. Convert to private key to p12 format for input into a JKS
```
openssl pkcs12 -export -inkey cdcprime.key  -in prime-cdc-gov-sender.pem -name CDCPRIME -out cdcprime.p12
```


Some helpful references
- https://www.digicert.com/kb/ssl-support/openssl-quick-reference-guide.htm
- https://www.kinamo.be/en/support/faq/useful-openssl-commands
- https://docs.oracle.com/cd/E35976_01/server.740/es_admin/src/tadm_ssl_convert_pem_to_jks.html
