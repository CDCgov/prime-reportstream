# SFTP Configuration

## Introduction
All of the files in this folder are for use with the atmoz SFTP server in
our development environment, and are *NOT* intended to be used in production.

So what is happening here? Well, we have a mix of different authentication methods we use
when connecting to clients. In some cases we use password auth, but in other cases we
use public key auth. Up until now, our integration tests *ONLY* checked password auth,
meaning we have been vulnerable to breaking changes affecting our ability to connect to
clients. In fact, that did happen in November 2021. This change, and the files in this folder
are an attempt to add in public key auth, so we are less likely to approve changes that
break our ability to operate.

In specific, when Docker starts, atmoz will pull the files from this folder and will map
them into the correct locations onto the SFTP server so they are usable.

## File Details
Below is an explanation of each file.

- ssh_dev_rsa_key: This is the private key you use to connect to the SFTP server as the `bar` user
- ssh_dev_rsa_key.pub: This is the public key for above that the server uses to verify the identity of `bar`
- ignore-putty-file-old: A PuTTy key file that contains both the private and public key. It is in the v2 format for PuTTy, which is the most common format sent to us by our partners. Used by the `fizz` user.
- ignore-putty-file-old-openssh.pub: The extracted public key from the `ignore-putty-file-old` key file, but in the open-ssh format, which is the format most commonly accepted by SFTP servers. Used to verify the identity of the `fizz` user.
- ssh_host_ed25519_key: A permanent host key so we no longer get MITM attack warnings when connecting to our local SFTP server
- ssh_host_ed25519_key.pub: The public key part of above
- ssh_host_rsa_key: The same as the ed25519 key, but in the RSA format
- ssh_host_rsa_key.pub: See above
- users.conf: The users available to the SFTP server. Note that in the users.conf file, we do not specify a password for the users `bar` and `fizz`. This is because they are intended to use public key auth.

## Extra notes
The keys for above will need to have their permissions set to `0600` before they can be used.

Refer back to the docker compose for how the above is used.

I've exempted this entire folder from the gitleaks check because it's just used for dev work. **Caveat Emptor**

## Set Up
In order to make the two new tests work, you will need to run the following commands:
```shell
# export the vault
export $(xargs < .vault/env/.env.local)
# add the ppk file type to the vault
./prime create-credential --type UserPpk --ppk-user fizz --ppk-file-pass changeIT! --ppk-file ../.environment/sftp-conf/ignore-putty-file-old
# add the pem file type to the vault
./prime create-credential --type UserPem --pem-user bar --pem-file-pass changeIT! --pem-file ../.environment/sftp-conf/ssh_dev_rsa_key.pem
```

_If you don't do this, your tests won't work. I can't help you._

## Sample Docker Compose

```yaml
#local SFTP server as a receive point
  sftp:
    image: atmoz/sftp
    ports:
        - "2222:22"
    volumes:
      - ./build/sftp:/home/foo/upload
      # add more users
      - ../.environment/sftp-conf/users.conf:/etc/sftp/users.conf:ro
      # add host keys so we're the same all the time and no MITM messages
      - ../.environment/sftp-conf/ssh_host_ed25519_key:/etc/ssh/ssh_host_ed25519_key
      - ../.environment/sftp-conf/ssh_host_rsa_key:/etc/ssh/ssh_host_rsa_key
      # add a key for the dev
      - ../.environment/sftp-conf/ssh_dev_rsa_key.pem.pub:/home/bar/.ssh/keys/ssh_dev_rsa_key.pem.pub:ro
      - ../.environment/sftp-conf/ignore-putty-file-old-openssh.pub:/home/fizz/.ssh/keys/ignore-putty-file-old-openssh.pub:ro
    networks:
      - prime-router_build
```

## Sample users.conf
```shell
# our default user, who logs in via a password
foo:pass:1001:100:upload
# no password for bar. they use public key auth
bar::1002:100:upload
# no password for fizz. they use public key auth
fizz::1003:100:upload
```

## Nitty-Gritty: What Is This All About Really?
**tl;dr** Encryption is a complex topic, and I don't have time to go into detail, but here's a
very brief introduction into how it works in our case, and what those files are actually for.

### Disclaimer
If you don't understand the key concepts of encryption and cryptography, there is not much I can
do to help you with that. I barely understand it myself. There are many wonderful and insightful 
books and videos and tutorials created by people who are smarter than I by orders of magnitude,
and it would be much better to learn about all of this from them.

Suffice it to say, I'll try to cover a few important details here so it is slightly less foggy for all.

### Background
Once upon a time, connecting to a service over the internet was very straightforward and easy. Tools
like `telnet`, `rsh`, and `ftp` made it very easy to send and receive data long distances. They had a very
serious shortcoming though: everything you sent was sent over clear text, which meant that anyone
sitting in the middle of your connection could read what you were sending.

This meant that if Alice and Bob were chatting over telnet, Eve could passively listen in with little
difficulty.

In order to combat this, the IETF released the Secure Shell (SSH) cryptographic network protocol in 1995, which
was followed by the SSH File Transfer Protocol (SFTP) in 1997.

At its core, SSH uses public-key cryptography (aka asymmetric encryption) for authentication.

### Asymmetric What?
Asymmetric encryption. There are several ways to encrypt data, but two of the most common are symmetrical
and asymmetrical encryption. Consider Alice and Bob again. They have a secret that they want to share with each other. 
One way to accomplish this would be to encrypt the message but share the same key. This means that both Alice
and Bob have the same key decrypt the data. This is like everyone having the same key to get into the house.
If someone has a key, they can get in.

In asymmetric encryption, both Alice and Bob have their own private key, and they have a public key. They can
share their public keys with each other without compromising or risking their private keys. Alice can use Bob's 
public key to encrypt a message and send it to him. With his private key, Bob can decrypt and read the message.
He can then use Alice's public key to encrypt a response and send it to her, and only she will be able to
decrypt it.

As long as both parties keep their keys secure (and they are using a modern encryption method), there is 
very little chance that their messages can be cracked by Eve, listening in the middle.

There is one major downside to asymmetrical encryption: it is a lot slower to use.

For this reason, SSH uses public keys to authenticate the user, and then creates a new symmetrical key to
communicate for that session. Once the session is over the symmetrical key is disposed of.

This is why the servers we communicate with (or our local dev server) only needs the public portion of the
key for authentication. When our code attempts to connect to a server to send data, it identifies itself
by the public key. The server then checks to verify that someone is authorized to use that public key to log
in, and generates an encrypted challenge message to our code. Our SSHJ library then decrypts said challenge
message using the private portion of the key, and returns it to the server, proving that we are the owner 
of the key.

### So Why Two Different File Types
We deal with both `pem` and `ppk` file types because there are different competing options for how to do
encryption and SSH on different platforms. On Windows, WinSCP and PuTTY are typically preferred, while on
Linux and MacOS and other Unix-like systems `openssh` is the de facto standard. Each has their own defined
format for the key files they use, and beyond that there are standards organizations that have put forward
their own specifications.

#### PuTTY
A typical PuTTY key file looks like this. This comes from the `ignore-putty-file-old` key file in this folder:
```text
PuTTY-User-Key-File-2: ssh-rsa
Encryption: aes256-cbc
Comment: rsa-key-20211229
Public-Lines: 12
AAAAB3NzaC1yc2EAAAADAQABAAACAQCNgkqLaH09F+5FBrX0a9JD/xkWJBJMp9iY
uiZ83rEcyWWD9aeEGAWnMT3LJoy4kwa6+HndO1wJSCAInWQphY8crATYfn+9c0eD
HgDi8thOkPp/tkocUt3WyRs+rTEZ5CcxFTeW/qs1bTDfc7e5ZYNPER9Pq564xCJf
ghC51BakAGDxoQPq3pJvQQvxWyrPKvVOZXkMyD6ePIFgYVd8vmHsTSOdF2Le+Oyx
k3YHhxWWb6h0k+FGV1YNmSVjZctsZuxWmoLglLqcDsIerjAPpeBsebS4DlZlBuBY
FPtLvKCNacCp2GuLN2g1Hchm8tGbU6S2XPuO3QzEJOTqxZZZiyMN04OuCgkcW0ss
+1mknkxDbx1fmbH5n0Zh/owdkqRU1/FYiWimHb65Cuj+x0oGrjERJHiARH8Rroi1
K9J2vIqiGDaIBWR616Pu0l4t+ryD+eERdYW672sfFnMgMetwlzjpO6vTVnwE8PmG
KMWQfpYUuyFm3eK0rlQxWhwap824wDjlmt8s23MEvBUb6T7sbM5gu6ckBYKGiftQ
aXLc8EzqpJkGUb3rmfgmDHzpj56frL5IPHJYD1o69SHrP4tgftPNqu4OTGrWvngS
h3ykul7+llhRwCypEKxxJx5pvfHnBjqgwz2pzvf9bgmBTkSSv0idwpMlraayzTRA
8GvJpiCgxw==
Private-Lines: 28
kfehgrZZDcj6RAErTSqYAvwfMkXaMN9YCHSaKrZS5YwhrS5sCjLweTy9DDqrLov1
+rWGEpXDQkL4L13GIBbeLbc5Bpsbyf0Umk0UtAGnQVIXyT7McMw6foowrPO4UZgX
E7d4bUioiMmtxm98pKSvGE6SX7PqgvEmUAL6Ay3UI4B+BYkCYTi5PAZ4SY39BG53
Iqmri2bFuYWIcErlaR58Ro7f2HnrxEvkOWnve9hUkmneSvI9eCDqtkch3uRaEkdl
voyVVeHDqDXJS62sT43GV7KM3Lac2alN6wQvMdhvb2JD9lz3NpQhwbZbeBsBD7MP
L+fxJI9D3SBHWRWnZt4oQSSC4F+rzzOQKHNfMv112dJoOzLQd8yt3VOaE1tKzLJU
Ffsl1LrUvwMnk42T3yimnVYPz4Ye8jyaVgXINeHuOrYW46OeWwtRI+UirTk9gHlm
EE7YguuKVqkT6txLJsTfBsYc4yNSGe7k0hXAFHXBYWgqKYd+BecETOdreAqTh4mS
fT9fyaQ59rWOAQ/iOiGR6eGRoyQMuvnKnvbaEhyrsxQpZytdby5S1J5Kl+Yh7rZv
o2AKjW7B7/DTdbTvkMkhrx6yAZi/2/V0+815iNG6BH63Fbd4fLF/bTtSZ+P0Uk4K
5rzsFXhHCG48T+Fit74h5Ybp3+dgkN5FZdeuw5orAbHCZCOsMSOrqWOmtetPpEik
YfG76w5DSzh2NHQ+H9+Md/feEBn6X8DBmqlEC52EpUGtQxdEx+MRFAzRwF4bxD06
UsG616tuE/qjLyYzyIlhH+qQbh9W/qDGh+BCesj6Du8fKRKGcv2X9mxSSaILDLMJ
dl5FHtoIGLzloXNYKBoWG0TO9QPqTCEmlGgibkQbzH3XNNS6vyRb2Q7kej9aM91p
rF26UASE3LTFAJy+CQ4cOLZQgBNnliDsp2poLJTxJQn1B2M+Jt6wkIonZQF8ffR/
nTeFNx18EEqTEQjuFArH5f8CUkmQwz0cGSi4tQi2k1YymfL9pdWVuBhtw4hPEeZf
DqDWb5/3NlrHO1+9CRY1GFj8s0g0hK0f7UI1dtdCcZbQO/KLlu+d8rVZa0CT9AvH
ETfwbvCUCvCh+2G+8lhGqDLxHlNTUix1uV/pqPNK4nSPnWoJl0nEXrpwuJj5Zsd4
j5xK6PN6hL7ABymirJFdBY78Q6weyF5t1eC96322bg2TjwcXd2mzfv8GlBs2KHzG
jjBrYC91L69SfywgF/1qG4iK151MM/Hcy4te7hFn1l3qhMN4soewHrGdSPiG8NsD
tmjbB+4BhpU5tdmDv+cIesieJ20UsMDbM6znvU2L6ekTKvvIXhDk8gKqsDePUrpB
gctszWVbEnBtO1iqcLUiaFVZQYIjEPZYyL54utWzvBrvVjnldFHIsovIvLpSOuA3
ZVVlPbqxabnOMFDWGF064kjDJQ/auzzjqZlpYsTNy9tuduWek1LQXGk0/bpZk2wV
zPIhmm65ALmJk8D6XQBb66UNo1o/IwtKpgNTXFg4KlWdIjglRj1AIBt1/pMKf1TH
kM98PSyJUAfdfC2nTLARtJ+0IMFhIIucd3xPzTjJZZuUxzDt+wvroP5WIzGW0FVA
P2WADM4uRiaeVdGn0qApJXEML+fnLAUKCMv62rni4bg9qpDH/yFhw9wizI+wbAVo
xZtO3lkpto9DM06LtHZUIKNuEy8TET35lkM/A4ToLN4YWNVJvuelz6cC6VlkDbHE
RaR7k6vJ5nfWn7F1jIBYCA==
Private-MAC: 22efd1c2f8b247aa10a5fd4dfd10b78c6d389eae
```

## Additional Documentation
- https://hub.docker.com/r/atmoz/sftp