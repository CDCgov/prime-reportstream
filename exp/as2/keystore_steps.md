The keystore for the program will contain many parts

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

3. Create a empty WA OHP JKS (password ChangeIt!) 
```
keytool -genkey -keyalg RSA -alias endeca -keystore waohp.jks 
keytool -delete -alias endeca -keystore waohp.jks 
```

4. Convert to private key to p12 format for input into a JKS
```
openssl pkcs12 -export -inkey cdcprime.key  -in prime-cdc-gov-sender.pem -name CDCPRIME -out cdcprime.p12
```

5. Import the private key into the keystore. Matches the AS2ID
```
keytool -importkeystore -srckeystore cdcprime.p12 -srcstoretype PKCS12 -destkeystore waohp.jks -deststoretype JKS
```

6. Import the OPH cert to the keystore
```
keytool -import -alias as2ohp -file as2OHP.cer -keystore waohp.jks
```

7. Check
```
$keytool -list -keystore waohp.jks
Enter keystore password:
Keystore type: PKCS12
Keystore provider: SUN

Your keystore contains 2 entries

as2ohp, Apr 3, 2021, trustedCertEntry,
Certificate fingerprint (SHA-256): 40:09:F2:0F:32:5E:45:5B:5F:53:DE:E3:88:83:23:60:47:AA:07:D5:55:3B:12:4E:18:21:75:E9:56:FC:58:67
cdcprime, Apr 3, 2021, PrivateKeyEntry,
Certificate fingerprint (SHA-256): 0E:0A:66:B0:A1:8A:9C:0F:C1:8C:EF:50:39:B9:AC:10:C7:D5:DB:29:D7:5E:B1:F7:BD:AC:00:AC:4A:CA:CE:CC
```


Some helpful references
- https://www.digicert.com/kb/ssl-support/openssl-quick-reference-guide.htm
- https://www.kinamo.be/en/support/faq/useful-openssl-commands
- https://docs.oracle.com/cd/E35976_01/server.740/es_admin/src/tadm_ssl_convert_pem_to_jks.html
