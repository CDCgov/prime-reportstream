# Architecture

A separate VPN profile is used for each Azure VNET. This is to ensure there is no cross-talk between Azure VNETs.

# Download a VPN Client

The VPN uses OpenVPN to connect with certificate-based authentication. You will need an OpenVPN client. Since OpenVPN works over port 443, OpenVPN should work even in the most complex government networks.

Recommended clients:

- Mac : [Tunnelblock](https://tunnelblick.net/index.html)
- Windows: [OpenVPN Client](https://openvpn.net/community-downloads/) - Do not use [OpenVPN Connect](https://openvpn.net/client-connect-vpn-for-windows/) as it is unable to verify the server's identity
- Linux: [OpenVPN](https://openvpn.net/)

# Using the VPN

You will be provided a VPN profile that is unique to you for each environment. The profile will include all keys, certificates and settings required to connect to the VPN Gateway and route the appropriate traffic through it.  Maintain these profiles secured as they contain your credentials to login to the VPN.

## Using your Profiles

Once you receive your VPN profile, import the profile into the OpenVPN client of your choice.  See the clients documentation for more information.

## Linux

Store the `.ovpn` file(s) only on a trusted device, in a secured location to which only your user has read access (e.g. `/home/${USER}/.openvpn/`). Anyone who gets access to any one of these `.ovpn` files effectively becomes _you_ and leaves an audit trail pointing at _you_.

Due to split-DNS routing, out-of-the-box NetworkManager will *not* work; instead invoke the client from the command line as follows:
```bash
# This will open the VPN tunnel and make the process just sit there
# Terminate it with Ctrl+C
$ openvpn --config "<path-to-ovpn-file>"

# Alternatively

# This will open the VPN tunnel, write the PID to "/tmp/${USER}/openvpn.pid" and return
# NOTE: this assumes you're not prompted for your sudo password
$ sudo openvpn --config "<path-to-ovpn-file>" --writepid "/tmp/${USER}/openvpn.pid" &
# Take down the VPN tunnel using
$ SIGNAL=TERM # or 'INT'
$ kill -${SIGNAL} $(cat "/tmp/${USER}/openvpn.pid")
# On termination, openvpn will remove the PID file
```

## Troubleshooting
- **Trouble Accessing Items in the Azure Portal?**

Some browsers like Firefox or Chrome have started defaulting to use custom DNS-over-HTTPS (DOH) providers. This will interfere with the VPN's DNS server. You will need to disable DOH on your browser. Directions below:

https://learn.akamai.com/en-us/webhelp/enterprise-threat-protector/etp-client-configuration-guide/GUID-04D2A852-CB51-4210-9CE3-7F6ABB3B84E2.html

- **I am getting an error on Windows when connecting: ssl routines tls_process_server_certificate certificate verify failed**

If you are using OpenVPN Connect, uninstall it and install the [OpenVPN Client](https://openvpn.net/community-downloads/).  
 

# For VPN Administrators
## Generate a VPN Profile

To generate keys for a VPN profile, follow the below steps. These steps [are derived from this Azure document](https://docs.microsoft.com/en-us/azure/vpn-gateway/vpn-gateway-certificates-point-to-site-linux).

* Obtain the root VPN keys `caKey.pem` and `caCert.pem`
* Generate a user certificate:

```
export VPN_USERNAME="client"

ipsec pki --gen --outform pem > "${VPN_USERNAME}Key.pem"
ipsec pki --pub --in "${VPN_USERNAME}Key.pem" | ipsec pki --issue --cacert caCert.pem --cakey caKey.pem --dn "CN=${VPN_USERNAME}" --san "${VPN_USERNAME}" --flag clientAuth --outform pem > "${VPN_USERNAME}Cert.pem"
```

* Generate a p12 bundle from the user certificate:

```
openssl pkcs12 -in "${VPN_USERNAME}Cert.pem" -inkey "${VPN_USERNAME}Key.pem" -certfile caCert.pem -export -out "${VPN_USERNAME}.p12"
```

* In the VPN profile, add the contents of the following files to the specified sections:
    * `${VPN_USERNAME}Cert.pem` to `<cert></cert>`
    * `${VPN_USERNAME}Key.pem` to `<key></key>`
* Securely transmit the VPN profile to the recipient

## Revoke a VPN Profile

If a VPN profile needs to be revoked for any reason this can be done via Azure.

* Generate a thumbprint of the VPN profile's user certificate:

```
openssl x509 -in ${USEERNAME}Cert.pem -fingerprint -noout
```

* Add the thumbprint to `root_revoked_certificate` block of the Terraform [`virtual_network_gateway` resource](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/virtual_network_gateway)
