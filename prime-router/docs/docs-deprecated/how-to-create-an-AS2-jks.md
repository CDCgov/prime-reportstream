# How to create an JKS for an AS2 receiver

The AS2 protocol uses public key infrastructure. Each transaction involves two certificates:

- A private certificate to sign the payload
- A public trusted certificate to encrypt the payload 

Each party generates a private/public key pair, keeps the private as a secret. Before a transaction, the sender and receiver
exchange public certificates. We store our private key and the destination's public cert in a Java Key Store (JKS) file.
The JKS is used as the credential for a receiver using the AS2 transport. 

> Note: Many steps of this recipe could be skipped if one generated the CSR from the keytool. 
> However, this note documents what actually done. 

1. Generate a private key
```
openssl genrsa -out cdcprime.key 2048
```

2. Generate a CSR for Entrust, CDC's certificate vendor. 
```
openssl req -new -sha256 -key cdcprime.key -out cdcprime.csr
```

3. After submitting the CSR to Entrust. Download the .PEM file from Entrust
```
prime-cdc-gov-sender.pem
```

4. Send the public certificate to the receiving entity, WA OHP in this case. 

5. Create a empty WA OHP JKS (password ChangeIt!)
```
keytool -genkey -keyalg RSA -alias endeca -keystore waohp.jks 
keytool -delete -alias endeca -keystore waohp.jks 
```

6. Convert to private key to p12 format for input into a JKS
```
openssl pkcs12 -export -inkey cdcprime.key  -in prime-cdc-gov-sender.pem -name CDCPRIME -out cdcprime.p12
```

7. Import the private key into the keystore. Matches the AS2ID
```
keytool -importkeystore -srckeystore cdcprime.p12 -srcstoretype PKCS12 -destkeystore waohp.jks -deststoretype PKCS12
```

8. Import the OPH cert to the keystore
```
keytool -import -alias as2ohp -file as2OHP.cer -keystore waohp.jks
```

9. Check
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

10. Store the JKS into the credential service.
```
$ ./prime create-credential \
   --type UserJks --persist WA-DOH--ELR \
   --jks-user qom6 \ 
   --jks-file ~/projects/as2-exp/waohp.jks \
   --jks-file-pass ChangeIt! \
   --jks-private-alias cdcprime \
   --jks-trust-alias as2ohp
```

Some helpful references
- https://www.digicert.com/kb/ssl-support/openssl-quick-reference-guide.htm
- https://www.kinamo.be/en/support/faq/useful-openssl-commands
- https://docs.oracle.com/cd/E35976_01/server.740/es_admin/src/tadm_ssl_convert_pem_to_jks.html
