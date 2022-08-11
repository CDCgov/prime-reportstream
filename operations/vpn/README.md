# VPN

## Revoke certificate

### Requirements:
 * openssl
 * az

### Example:

```sh
vpn_user="user1"
env="test staging prod"

# keybase
cert_data=$(keybase fs read keybase://team/prime_dev_ops/vpn/$vpn_user/${vpn_user}Cert.pem)
# or local file
cert_data=$(cat ${vpn_user}Cert.pem)

./operations/vpn/revoke-env-certs.sh $vpn_user "$env" "$cert_data"
```