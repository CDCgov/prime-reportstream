# DNS in Azure

## Environments use CDC DNS

As of November 2021 we're no longer using Azure DNS servers to lookup addresses for internal resources. Instead we're using a CDC DNS server which has the mappings for our resources -- databases, queues, storage accounts, etc -- manually entered. This means if we add new resources -- e.g., a Redis cache -- we'll need to register its IP and name with the CDC DNS server.

To do so you'll need to send the mapping of IP address to name(s) to Richard Teasley. We'll need to do this **before** enabling applications to use these services to ensure the services can be reached.

Different environments have unique areas in the CDC address space. These **cannot** overlap -- unlike our internal IP address spaces which use `10.0.5.x` no matter which environment you're in, the CDC-issued addresses must be 

* Production - `172.17.7.0/25`
* Staging - `172.17.6.0/25`
* Test - `172.17.5.0/25`

## VPN uses dnsmasq

If you connect your development machine [to the VPN](vpn.md) you'll need to point requests for Azure resources to a small DNS server we run for this purpose. The configuration for these servers is in `operations/dnsmasq/config/{environment}/hosts.local`, and each one has a mapping of IP address (in the `10.0.5.x` address space) to Azure resource host names.

>After environment (re)provisioning, validate `hosts.local` IP addresses are valid by comparing to NIC private IPs

Finally, you OpenVPN configuration needs to point to this DNS server. Fortunately this is already done for you -- each profile you receive when you get a VPN account has a section like:

```
dhcp-option DNS 10.0.2.4
dhcp-option DOMAIN azure.com
dhcp-option DOMAIN azure.net
dhcp-option DOMAIN azurewebsites.net
dhcp-option DOMAIN windows.net
```

So this configuration is telling your OS that requests looking up IPs under (for example) `azure.com` should go to the DNS server running at `10.0.2.4`, which is our `dnsmasq` server.