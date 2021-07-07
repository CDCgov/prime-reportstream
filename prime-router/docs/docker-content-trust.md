% Signing Docker Containers Walkthrough

# Private Key Generation

This section walks you through how to create a new RSA Code Signing Key. We have already received this key from CDC so you _can_ skip this step but it is here for documentation purposes.

Create a file (`keygen.conf`) that contains the following content:
```
[ req ]
default_bits            = 4096              # RSA key size
encrypt_key             = yes               # Protect private key
default_md              = sha256            # Message Digest method to use
utf8                    = yes               # Input is UTF-8
string_mask             = utf8only          # Emit UTF-8 strings
distinguished_name      = codesign_dn       # DN template
req_extensions          = codesign_reqext   # Desired extensions

[ codesign_dn ]
# do not omit

[ codesign_reqext ]
keyUsage                = critical,digitalSignature
# This enables the key to be used to sign code
extendedKeyUsage        = critical,codeSigning
subjectKeyIdentifier    = hash
```

Generate the private key and Certificate Signing Request using this command:
```
openssl req -new -newkey \
    rsa:4096 \
    -keyout my_key.priv \
    -sha256 \
    -nodes \
    -out my_key.csr \
    -subj "/CN=Your Common Name Here" \
    -config "keygen.conf"
```

Explanation:

* `req -new -newkey`: We are initiating a new certificate signing request for a new key
* `rsa:4096`: Generate an RSA key that is 4096 bits
* `-keyout my_key.priv`: output the key to `my_key.priv`
* `-sha256`: use SHA256 as Message Digest method
* `-nodes`: Do not encrypt the output key (maybe stands for 'no DES'?); you could omit this but then you'll be prompted for a password every time you need to unlock the private key
* `-out my_key.csr`: output the Certificate (Signing) Request to `my_key.csr`; by convention this is named similar to the private key file.
* `-subj "/CN=Your Common Name Here"`: The 'subject' (i.e. the '_who_') of your certificate; in this case, we specify our Common Name (CN) as 'Your Common Name Here'
* `-config "keygen.conf"`: Use the configuration as speficied in `keygen.conf`

You can show the Certificate (Signing) Request info using:

```
openssl req -in my_key.csr -noout -text
```

Explanation:

* `req`: This deals with request
* `-in my_key.csr`: the input file is `my_key.csr`
* `-noout`: do not output the request itself
* `-text`: show text form

# Public Key Derivation from Private Key

This section explains how to get a public key from the private key you either received or generated in the section above.

Execute the following command
```
openssl rsa \
    -in "my_key.priv" \
    -pubout \
    > "my_key.pub"
```

Explanation:

* `rsa`: We are doing RSA-based key stuff
* `-in "my_key.priv"`: Use the private key, stored in `my_key.priv`, as input
* `-pubout`: Output a public key to stdout
* `> "my_key.pub"`: take the output to stdout and write it to a file called `my_key.pub`

# Docker Content Trust (DCT)

## Load the Private Key
Now that you have a private and public key pair, you need to tell Docker about it.
First, load the private key into your Docker Trust Store, and in this process, you will be asked to enter a password for the key.

```
docker trust key load \
    --name "codesigning-key" \
    "my_key.priv"
```

Explanation:

* `trust key load`: we are loading a new key into the Trust Store
* `--name "codesigning-key"`: The name of this key is `codesigning-key`
* `"my_key.priv"`: the key itself can be found in the file `my_key.priv`


## Delegate signing using the public key

This section tells docker that when you are trying to sign this particular image, which key to use:

```
docker trust signer add \
    --key "my_key.pub" \
    "orgname-signer" \
    "your.container.repo/image-name"
```

You will be prompted to set up passwords to 2 keys in this process (the first time you do this for an image):

* **root key** (aka 'offline key'): The root of content trust for an image tag. Docker can read the password for this key from the `DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE` environment variable.
* **repository key** (aka 'targets key'): Enables you to sign image tags. Docker can read the password for this key from the `DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE` environment variable.

Explanation:

* `trust signer add`: we are adding a signer to the trust store
* `--key "my_key.pub"`: Signatures should use this public key
* `"orgname-signer"`: This is who '_we_' are, i.e. who are we signing as
* `"your.container.repo/image-name"`: This aplies to signing attempts of this _tagless_ image. In other words, if I try to sign `your.container.repo/image-name:v1.2.3`, then the public key from `my_key.pub` will be used because the image name matches.

# Signing Container Images

You can sign any built image, provided you have a valid key. To sign an image `your.container.repo/my-image:v1`, you'd go about it as follows:

```
# Build the image (assuming you have a Dockerfile in the current directory)
# and tag it with your.container.repo/my-image:v1
docker build -t your.container.repo/my-image:v1 .

# Sign the image
# This uses the signer for 'your.container.repo/my-image'
docker trust sign your.container.repo/my-image:v1
```

You will be asked for the password to the (private) key you loaded earlier (cf. [Load the Private Key](#load-the-private-key) ). Alternatively, docker can use the value from the environment variable called `DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE` as the password value.

**NOTE** however that the _value_ for the environment variable called `DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE` is **_NOT_** the same as the value described in [Delegate signing using the public key](#delegate-signing-using-the-public-key) and _instead_ should be set to the value as specified for the private key when loading it into the trust store:

```
DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE=key_not_repo_password \
    docker trust sign your.container.repo/my-image:v1
```


Note that on (successful) signing, the signed image is pushed into the (remote) repository (i.e. `docker push`).

# Inspecting Signatures

You can see which signatures apply to an image (tagged or tagless) using these commands:

```
# Show all known signatures for your.container.repo/my-image
docker trust inspect --pretty your.container.repo/my-image

# Show only signatures on the v3 tag of your.container.repo/my-image
docker trust inspect --pretty your.container.repo/my-image:v3
```

The `--pretty` flag makes the output human-readable, omitting it outputs JSON (which could be used in scripts and interrogated further with the `jq` tool, e.g.: `docker trust inspect your.container.repo/my-image:v3 | jq .[0].Signers[].Name`, etc.).

# Enforcing Content Trust

By default, docker does not enforce content trust, you can tell it to do so by setting the `DOCKER_CONTENT_TRUST` environment variable to `1`. A repository may contain both signed and unsigned images and clients that do _not_ enforce Content Trust can continue to pull down both signed and unsigned images. It's only clients that enforce Content Trust that are 'limited' (by their own volition) to be able to pull down only signed images.
Additionally, it is good to be aware that signed images that have been pushed into your registry may be overwritten with other images at a later point in time. These overwriting (younger) images may be unsigned, signed with the same key, or signed with another key.

```
# inline
DOCKER_CONTENT_TRUST=1 docker pull "your.container.repo/my-image:latest"

# explicit
export DOCKER_CONTENT_TRUST=1
docker pull "your.container.repo/my-image:latest"
```

Unsigned images will **not** be pulled down when this is set. You will receive an error message that says
`Error: remote trust data does not exist for docker.io/tddocker0/ytdl-webui: notary.docker.io does not have trust data for your.container.repo/my-image:latest` and the return code for the docker process will be set to `1` indicating failure.

# `make clean`

In the event that something gets seriously borked up, you can 'reset' your Docker Content Store by:

```
rm -rf "${HOME?}/.docker/trust"
```

Note that this blows away any local trust information you have stored (keys, signers, etc...) but does NOT eliminate any trust configurations on the 'server side' (i.e. your container registry itself). In other words if a password has been set up for the container registry, you'll obviously need to remember it.