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

If you're not able to get connectivity to services in Azure after the VPN has started, and/or you see the message
`Warning: /etc/resolv.conf is not a symbolic link to /run/resolvconf/resolv.conf.` you may need to give your
systemd-based resolv.conf services a kick:

```bash
$ sudo dpkg-reconfigure resolvconf
```

This will walk you through a curses-based UI, just hit OK to everything. It'll tell you you need to reboot, and that may
be true sometimes but it wasn't for me.

More info on [this issue](https://github.com/pop-os/pop/issues/773).


## Windows

### VPN DNS Resolution

There is an issue where the DNS server for the VPN adapter is not used when resolving hostnames.  This is due to the interface metric of the VPN loosing to the metric of your normal network adapter.  Run the following command to look up the IP of a server in the Azure environment and test if this is an issue .  For example:

```
nslookup pdh<env>-pgsql.postgres.database.azure.com
For example:
nslookup pdhstaging-pgsql.postgres.database.azure.com
```

should return an IP address in the 10.0.0.0/8 range which is in the range used by the VPN.  If you see an address outside of this 10.0.0.0/8 range then continue with the instructions here to fix this issue.

To fix this issue:

1. Open Control Panel as an administrator
2. Choose Network and Internet, and then choose Network Connections.
3. Right-click the TAP-Windows Adapter V9 tap adapter.
4. Choose Properties, and then choose Internet Protocol Version 4.
5. Choose Properties, and then choose Advanced.
6. Clear the Automatic Metric box.
7. Enter 1 for Interface Metric.
8. Choose OK.

Run the Windows command `netsh interface ip show config` to verify the InterfaceMetric value has change to 1 for the OpenVPN TAP-Windows interface.  For example:

```
Configuration for interface "OpenVPN TAP-Windows6"
    DHCP enabled:                         Yes
    IP Address:                           192.168.10.5
    Subnet Prefix:                        192.168.10.0/24 (mask 255.255.255.0)
    InterfaceMetric:                      1
    DNS servers configured through DHCP:  10.0.2.4
    Register with which suffix:           Primary only
    WINS servers configured through DHCP: None
```

Reference: https://aws.amazon.com/premiumsupport/knowledge-center/client-vpn-fix-dns-query-forwarding/ - it says that changing this setting via the control panel does not work, but this has been confirmed to work.

## Troubleshooting
- **Trouble Accessing Items in the Azure Portal?**

Some browsers like Firefox or Chrome have started defaulting to use custom DNS-over-HTTPS (DOH) providers. This will interfere with the VPN's DNS server. You will need to disable DOH on your browser. Directions below:

https://learn.akamai.com/en-us/webhelp/enterprise-threat-protector/etp-client-configuration-guide/GUID-04D2A852-CB51-4210-9CE3-7F6ABB3B84E2.html

- **I am getting an error on Windows when connecting: ssl routines tls_process_server_certificate certificate verify failed**

If you are using OpenVPN Connect, uninstall it and install the [OpenVPN Client](https://openvpn.net/community-downloads/).  
 

# For VPN Administrators

## Generate a VPN Profile (Automatic)

1. Download the following files from our Keybase team `prime_dev_ops`: [keybase://team/prime_dev_ops/vpn](keybase://team/prime_dev_ops/vpn)
   * `caCert.pem`
   * `caKey.pem`
   * `prime-data-hub-prod.ovpn`
   * `prime-data-hub-staging.ovpn`
   * `prime-data-hub-test.ovpn`
   * `createKey.sh`
   > If **new** Virtual Network Gateway, update the appropriate `.ovpn` file above with new `remote`, `verify-x509-name`, and `<tls-auth>` values.
2. Run `createKey.sh` and follow the prompts
3. The user's VPN profile with be outputted in a folder with their name
4. Securely transmit the VPN profile to the recipient

## Generate a VPN Profile (Manual)

To generate keys for a VPN profile, follow the below steps. These steps [are derived from this Azure document](https://docs.microsoft.com/en-us/azure/vpn-gateway/vpn-gateway-certificates-point-to-site-linux).

* Obtain the root VPN keys `caKey.pem` and `caCert.pem`
  * If you need new root VPN keys, run the following to generate a new public root cert base64 to add to the Virtual Network Gateway:
    ```bash
    ipsec pki --gen --outform pem > caKey.pem
    ipsec pki --self --in caKey.pem --dn "CN=VPN CA" --ca --outform pem > caCert.pem

    openssl x509 -in caCert.pem -outform der | base64 -w0 ; echo
    ```
  * If replacing an existing public root cert base64, it may not apply unless you first delete the pre-existing instead of overwriting.
  * [Resource](https://docs.microsoft.com/en-us/azure/vpn-gateway/vpn-gateway-certificates-point-to-site-linux#cli)
* Generate a user certificate:

    ```bash
    export VPN_USERNAME="client"

    ipsec pki --gen --outform pem > "${VPN_USERNAME}Key.pem"
    ipsec pki --pub --in "${VPN_USERNAME}Key.pem" | ipsec pki --issue --cacert caCert.pem --cakey caKey.pem --dn "CN=${VPN_USERNAME}" --san "${VPN_USERNAME}" --flag clientAuth --outform pem > "${VPN_USERNAME}Cert.pem"
    ```

* Generate a p12 bundle from the user certificate:
    ```bash
    openssl pkcs12 -in "${VPN_USERNAME}Cert.pem" -inkey "${VPN_USERNAME}Key.pem" -certfile caCert.pem -export -out "${VPN_USERNAME}.p12"
    ```

* In the VPN profile, add the contents of the following files to the specified sections:
    * `${VPN_USERNAME}Cert.pem` to `<cert></cert>`
    * `${VPN_USERNAME}Key.pem` to `<key></key>`
* Securely transmit the VPN profile to the recipient

**Note:** GitHub environment VPN credentials are [stored as base64](https://github.com/golfzaptw/action-connect-ovpn#how-to-prepare-file-ovpn).

## Revoke a VPN Profile

 * [Instructions](../../operations/vpn/README.md)