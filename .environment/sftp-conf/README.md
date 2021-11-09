# SFTP Configuration

## Introduction
All of the files in this folder are for use with the atmoz SFTP server in
our development environment, and are *NOT* intended to be used in production.

## File Details
Below is an explanation of each file.

- ssh_dev_rsa_key: This is the private key you use to connect to the SFTP server as the `bar` user
- ssh_dev_rsa_key.pub: This is the public key for above that the server uses to verify the identity of `bar`
- ssh_host_ed25519_key: A permanent host key so we no longer get MITM attack warnings when connecting to our local SFTP server
- ssh_host_ed25519_key.pub: The public key part of above
- ssh_host_rsa_key: The same as the ed25519 key, but in the RSA format
- ssh_host_rsa_key.pub: See above
- users.conf: The users available to the SFTP server

## Extra notes
The keys for above will need to have their permissions set to `0600` before they can be used.

Refer back to the docker compose for how the above is used.

I've exempted this entire folder from the gitleaks check because it's just used for dev work. **Caveat Emptor**

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
      - ../.environment/sftp-conf/ssh_dev_rsa_key.pub:/home/bar/.ssh/keys/id_rsa.pub:ro
    networks:
      - prime-router_build
```