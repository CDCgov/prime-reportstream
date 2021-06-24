# Palo Alto - CDC IP Space Proposal

## Context and Motivation

We are required to implement a firewall system provided by Palo Alto Networks as part of our ATO. The Palo Alto firewall exists in an Azure VNET that is peered with the CDC intranet.

To utilize the Palo Alto firewall, we are required to peer our VNET with Palo Alto firewall VNET, so our egress traffic can filter through the firewall. In Azure, [peering VNET requires that the IP space does not overlap](https://docs.microsoft.com/en-us/azure/virtual-network/virtual-networks-faq#can-i-peer-two-vnets-with-matching-or-overlapping-address-ranges).

Unfortunately, since our project was originally setup in the CDC DMZ, we have enjoyed full control of our IP space and have chosen to IP our resources under a `10.0.0.0/16` CIDR. This is a broad IP space and conflicts with the IP space of both the Palo Alto firewall VNET and the CDC intranet.

To ensure we do not have an overlapping IP range, we must be assigned an IP CIDR directly from the CDC. They will create a VNET with this IP CIDR for us and then we can redeploy our resources under this VNET.

### Scope of This Proposal

This proposal is to reach an understanding of the size of the IP CIDR we must request from the CDC for each environment.

This proposal *will not* address how we redeploy our resources to the new VNET nor will it address and Palo Alto firewall setup. Those items will be addressed in future proposals.

## Goals

* Determine an IP CIDR range to request from the CDC per VNET-environment pair
* The IP CIDR range must meet our current and future needs
* We should request the smallest range possible that will meet our needs
    * If we cannot provide justification to the range size with the CDC, our request may be rejected
* Understand how we will allocate the CIDR to our individual subnets

## IP Usage Today

Our Azure resources are deployed in two different regions to enable regional redundancy in the event of a region failure. Due to this, we will need to request two VNETs per environment.

For each subnet we create, [Azure reserves 5 IP addresses](https://docs.microsoft.com/en-us/azure/virtual-network/virtual-networks-faq#are-there-any-restrictions-on-using-ip-addresses-within-these-subnets). Therefore the smallest CIDR allowed for a subnet is `/29`.

Some subnets show 0 IPs in use outside of the reserved IP addresses. This is because the communication over the subnet is taking place with a reserved IP address [using Azure service endpoints](https://docs.microsoft.com/en-us/azure/virtual-network/virtual-network-service-endpoints-overview).

### East US VNET (`10.0.0.0/16`)

Our primary region.

#### Subnet Usage

| Subnet | Description | CIDR | IPs | Used | Available |
|:--|:--|--:|--:|--:|--:|
| public | Internet-routable resources | 10.0.1.0/24 | 256 | 5 + 0 = 5 | 251 |
| container | Docker containers (ex. SFTP test) | 10.0.2.0/24 | 256 | 5 + 2 = 7 | 249 |
| private | VNET-only, not Internet-routable | 10.0.3.0/24 | 256 | 5 + 0 = 5 | 251 |
| GatewaySubnet | Required for a VPN gateway; smallest allowed is `/27` | 10.0.4.0/24 | 256 | 5 + 1 per connection | - |
| endpoint | [Azure Private Endpoints](https://docs.microsoft.com/en-us/azure/private-link/private-endpoint-overview) | 10.0.5.0/24 | 256 | 5 + 8 = 13 | 243 |

#### VNET Summary

|--:|:--|
| 65,536 | IPs in CIDR |
| 1280 | Allocated IPs |
| 64,256 | Available IPs |

### West US VNET (`10.1.0.0/16`)

Our secondary region. Only includes a redundant database at this time.

#### Subnet Usage

| Subnet | Description | CIDR | IPs | Used | Available |
|:--|:--|--:|--:|--:|--:|
| private | VNET-only, not Internet-routable | 10.1.3.0/24 | 256 | 5 + 0 = 5 | 251 |
| endpoint | [Azure Private Endpoints](https://docs.microsoft.com/en-us/azure/private-link/private-endpoint-overview) | 10.1.5.0/24 | 256 | 5 + 1 = 6 | 250 |

#### VNET Summary

|--:|:--|
| 65,536 | IPs in CIDR |
| 512 | Allocated IPs |
| 65,024 | Available IPs |