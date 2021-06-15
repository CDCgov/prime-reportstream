## AS2 Experiment

This is simple AS2 client that sends a AS2 message

To setup an AS2 endpoint to receive a message I recommend [Django-PyAS2](https://django-pyas2.readthedocs.io/en/latest/index.html).
1. Follow the documents in the quickstart section to setup a server pair. This is also a good way to learn about the concepts of AS2. Send a test message between servers to test it out. 
2. Build key materials by follow the keystore instructions in [keystore_steps.md](keystore_steps.md)
3. Add a CDCPRIMETEST partner to the Django server
4. Check the constant that the prototype is using. Point to the local server. 
