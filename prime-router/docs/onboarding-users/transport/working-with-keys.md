### Working with Keys

#### Introduction
Each STLT has a unique configuration for server authentication. This documentation provides examples of how we've configured keys to successfully authenticate with different STLTs.

### Public/Private Key Pair
Most STLTs use public/private key pairs for authentication. Here's how to generate and configure these keys:

1. **Generate a PEM file:**  
   This command creates a PEM file containing both a private and public key:
   ```bash
   openssl genrsa -out my_rsa_private_key.pem 2048
   ```

2. **Extract the Public Key:**  
   To extract the public key from the PEM file and share it with the STLT, run the following command:
   ```bash
   ssh-keygen -y -f my_rsa_private_key.pem > my_rsa_public_key.pub
   ```

3. **Convert PEM to PPK:**  
   The PEM file needs to be converted into a PPK file and stored in Azure for ReportStream authentication. Use this command to convert the file:
   ```bash
   puttygen my_rsa_private_key.pem -o my_rsa_private_key.ppk
   ```
4. **Create ReportStream Credential:**
    Use the primeCLI create-credential command to store the ppk file in JSON to be able to store it in Azure so that ReportStream can use it.
   ```bash
    ./prime create-credential --type UserPpk  --ppk-file /Users/vic/Downloads/texas/tx_rsa_private_key.ppk
   ```

### STLT generates Public/Private key pair
Sometimes a STLT will generate and public private key pair and send them to ReportStream to authenticate. The format in which they are sent can differ from STLT to STLT.
Some STLTs will send a PFX file and that file will need to be converted to JKS so that ReportStream can use it.

1. **Convert PFX to JKS:**  
   This command creates a PEM file containing both a private and public key:
   ```bash
   keytool -importkeystore -srckeystore mypfxfile.pfx -srcstoretype pkcs12 -destkeystore clientcert.jks -deststoretype JKS
   ```
2. **Create ReportStream Credential:**
   Use the primeCLI create-credential command to store the jks file in JSON to be able to store it in Azure so that ReportStream can use it.
   ```bash
    ./prime credential-create --type UserJks --jks-use <IF APPLICABLE> --jks-file-pass <IF APPLICABLE> --jks-file <PATH to JKS File>
   ```

### Importing STLTs Self-Signed Certificate to ReportStream

As of the time this document was written, the process for importing self-signed certificates into ReportStream involves adding the certificate to ReportStream's Docker container.

1. **Add the Certificate:**
   Place the certificate in the following directory:
   ```
   prime-reportstream/prime-router/certs/
   ```

2. **Update the Dockerfile:**
   Add the certificate to the `prime-reportstream/prime-router/Dockerfile.dev` by including the following line:
   ```bash
   COPY ./certs/CDC-G2-S1.crt $JAVA_HOME/conf/security
   RUN cd $JAVA_HOME/conf/security \
   && $JAVA_HOME/bin/keytool -cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias <ALIAS> -file <PATH TO CERT>
   ```

  

