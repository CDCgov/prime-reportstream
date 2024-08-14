# SFTP Configuration

## Introduction
All of the files in this folder are for use with the atmoz SFTP server in
our development environment, and are *NOT* intended to be used in production.

_**LET ME SAY THIS AGAIN: DO NOT USE THESE KEYS IN PRODUCTION FOR ANYTHING. EVER.**_

So what is happening here? Well, we have a mix of different authentication methods we use
when connecting to clients. In some cases we use password auth, but in other cases we
use public key auth. Up until now, our integration tests *ONLY* checked password auth,
meaning we have been vulnerable to breaking changes affecting our ability to connect to
clients. In fact, that did happen in November 2021. This change and the files in this folder
are an attempt to add in public key auth, so we are less likely to approve changes that
break our ability to operate.

## File Details
Below is an explanation of each file's purpose.

- ssh_dev_rsa_key: This is the private key you use to connect to the SFTP server as the `bar` user
- ssh_dev_rsa_key.pem: This is the private key in the pem file format that is used by ReportStream to connect to the server as the `bar` user
- ssh_dev_rsa_key.pem.pub: This is the public key for above that the server uses to verify the identity of `bar`
- ignore-putty-file-old: A PuTTY key file that contains both the private and public key. It is in the v2 format for PuTTY, which is the most common format sent to us by our partners. Used by the `fizz` user.
- ignore-putty-file-old-openssh.pub: The extracted public key from the `ignore-putty-file-old` key file, but in the open-ssh format, which is the format most commonly accepted by SFTP servers. Used to verify the identity of the `fizz` user.
- ssh_host_ed25519_key: A permanent host key so we no longer get MITM attack warnings when connecting to our local SFTP server
- ssh_host_ed25519_key.pub: The public key part of above
- ssh_host_rsa_key: The same as the ed25519 key, but in the RSA format
- ssh_host_rsa_key.pub: See above
- users.conf: The users available to the SFTP server. Note that in the users.conf file, we do not specify a password for the users `bar` and `fizz`. This is because they are intended to use public key auth.

In specific, when Docker starts, atmoz will pull the files from this folder and will map
them into the correct locations onto the SFTP server so they are usable.

## Extra notes
The keys for above will need to have their permissions set to `0600` before they can be used.

If you are on a Mac, Linux, or other *nix-style system you cannot use the PuTTY key format in order to connect to the 
SFTP server. You will need to convert it to the OpenSSH `pem` format before usage.

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

### Asymmetric Encryption? Does that Mean There's Symmetric Encryption Too?
Yes. There are actually several ways to encrypt data, but two of the most common are symmetrical and asymmetrical
encryption. Consider Alice and Bob again. They have a secret that they want to share with each other. 
One way to accomplish this would be to encrypt the message but share the same key. This means that both Alice
and Bob have the same key decrypt the data. This is like everyone having the same key to gain entry to a house.
If someone has a key, they can get in, but you can't necessarily be sure who uses the key. If they key is shared
with more people, then more people have access.

In asymmetric encryption, both Alice and Bob have their own private key, and they have a public key. They can
share their public keys with each other without compromising or risking their private keys. Alice can use Bob's 
public key to encrypt a message and send it to him. With his private key, Bob can decrypt and read the message.
He can then use Alice's public key to encrypt a response and send it to her, and only she will be able to
decrypt it.

As long as both parties keep their private keys secure (and they are using a modern encryption method), there is 
very little chance that their messages can be cracked by Eve, listening in the middle.

_Side Note: It is possible for someone sitting in the middle to still crack the encrypted message, but it usually 
depends on the amount of computational power the attacker has and the strength of the key involved. Older, smaller
keys are more susceptible to being cracked. But this is beyond the scope of this README_

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
their own specifications. And, as noted above, on non-Windows systems, you cannot typically use the `ppk` 
format when connecting via SFTP. You must use the `openssh` format instead.

#### PuTTY
A typical PuTTY key file looks like this. This comes from the `ignore-putty-file-old` key file in this folder. As you
can see it contains both the private and public key. 
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

#### OpenSSH 
Below is the public key in the OpenSSH format:
```text
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDVt0Q+cx+rMy/j5hnSCU2JzmstXVHYj18Pwk6YQ9pqq2Ky/VDjrg4MLLh0ROeubKV3jXITix2j+ImrNFARo+ubRYRY9sUwxsxUwp67Trde62yjv6zJlh/Xkv6hAMflQtSbv/sI0owlRGkbtiCrNlB2xohh+IPJxX0ZfnQWCe/8nEkF3J5K7XoBLY5o1XyPkyL3rl75zfz+SOoqO5tKSUE9RvxqAOEUR8nvCFhucsdnpTrq2721yVK0NXJb5BKmM8ozM0vrTEkKrS/C1uexpgGnPkZ4Ewxveky2zlCDAazXIRiL2PVyqPkK89T4IQbpPeKO9pkZDvwpYqv7AoIiZ/EGQhXpiT+bmR2oGOZ2yfzTEIu9iE2saSXpRDivSUNXQPss1Lo+H+Cce6GEPG7WJTiESPCJmfqM9tJaL9oKsTaYhfQH7W+hLp+zeVFDeqsXG19EvRxFOQvggA9J5n8Ksu7MgjC3vASiuEbbW6bOeZ2v0bl2tzpcOmQKD8gUSjfvPKqk502t2rjX3JJ87iZEYxbi4hZeD88Q8gQEF6gCDCqqgFHi8+UHtz8HhSzcl1k/mTteJIVoOgq9kXfWPF6lAgB9W447y1Irl+5ECWwr3DhIre9f7x2m5UaebW0Y+8VKapm+r7rFQpJhsTCfluGxBw22i2JutEcQOm1S/tUNx2rnLQ==
```

And the private key in the OpenSSH format:
```text
-----BEGIN OPENSSH PRIVATE KEY-----
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
-----END OPENSSH PRIVATE KEY-----
```

And finally here is a `pem` file:
```text
-----BEGIN RSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: AES-128-CBC,7B9271EA68DFCC9DE296F0CB0C6070DD

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
-----END RSA PRIVATE KEY-----
```

## Conclusion
This change is a relatively minor update to how we are using the atmoz SFTP container, but gives us
a bit more test coverage for our common use cases. Because this can potentially cause test failures
it is good to have a basic understanding of how things are set up and why, but fully understanding
the intricacies of encryption requires more space and time than we are allowed.

## Additional Documentation
- https://hub.docker.com/r/atmoz/sftp