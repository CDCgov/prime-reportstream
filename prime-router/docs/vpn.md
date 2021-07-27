# Architecture

A separate VPN profile is used for each Azure VNET. This is to ensure there is no cross-talk between Azure VNETs.

# Download a VPN Client

The VPN uses OpenVPN to connect with certificate-based authentication. You will need an OpenVPN client. Since OpenVPN works over port 443, OpenVPN should work even in the most complex government networks.

Recommended clients:

- [Tunnelblock](https://tunnelblick.net/index.html) (Mac)
- [OpenVPN Connect](https://openvpn.net/client-connect-vpn-for-windows/) (Windows)

# Using the VPN

You will be provided a VPN profile that is unique to you for each environment. The profile will include all keys and certificates required to connect to the VPN Gateway.

Once you receive your VPN profile, import the profile into the OpenVPN client of your choice.

## Trouble Accessing Items in the Azure Portal?

Some browsers like Firefox or Chrome have started defaulting to use custom DNS-over-HTTPS (DOH) providers. This will interfere with the VPN's DNS server. You will need to disable DOH on your browser. Directions below:

https://learn.akamai.com/en-us/webhelp/enterprise-threat-protector/etp-client-configuration-guide/GUID-04D2A852-CB51-4210-9CE3-7F6ABB3B84E2.html

# Generate a VPN Profile

To generate keys for a VPN profile, follow the below steps. These steps [are derived from this Azure document](https://docs.microsoft.com/en-us/azure/vpn-gateway/vpn-gateway-certificates-point-to-site-linux).

* Obtain the root VPN keys `caKey.pem` and `caCert.pem`
* Generate a user certificate:

```
export USERNAME="client"

ipsec pki --gen --outform pem > "${USERNAME}Key.pem"
ipsec pki --pub --in "${USERNAME}Key.pem" | ipsec pki --issue --cacert caCert.pem --cakey caKey.pem --dn "CN=${USERNAME}" --san "${USERNAME}" --flag clientAuth --outform pem > "${USERNAME}Cert.pem"
```

* Generate a p12 bundle from the user certificate:

```
openssl pkcs12 -in "${USERNAME}Cert.pem" -inkey "${USERNAME}Key.pem" -certfile caCert.pem -export -out "${USERNAME}.p12"
```

* In the VPN profile, add the contents of the following files to the specified sections:
    * `${USEERNAME}Cert.pem` to `<cert></cert>`
    * `${USEERNAME}Key.pem` to `<key></key>`
* Securely transmit the VPN profile to the recipient

# Revoke a VPN Profile

If a VPN profile needs to be revoked for any reason this can be done via Azure.

* Generate a thumbprint of the VPN profile's user certificate:

```
openssl x509 -in ${USEERNAME}Cert.pem -fingerprint -noout 
```

* Add the thumbprint to `root_revoked_certificate` block of the Terraform [`virtual_network_gateway` resource](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/virtual_network_gateway)
