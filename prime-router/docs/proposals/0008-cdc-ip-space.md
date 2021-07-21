# Palo Alto - CDC IP Space Proposal

## Context and Motivation

We are required to implement a firewall system provided by Palo Alto Networks as part of our ATO. The Palo Alto firewall exists in an Azure VNET that is peered with the CDC intranet.

To utilize the Palo Alto firewall, we are required to peer our VNET with the Palo Alto firewall VNET, so our egress traffic can filter through the firewall. In Azure, [peered VNETs require non-overlapping IP spaces](https://docs.microsoft.com/en-us/azure/virtual-network/virtual-networks-faq#can-i-peer-two-vnets-with-matching-or-overlapping-address-ranges).

Unfortunately, since our project was originally setup in the CDC DMZ, we have enjoyed full control of our IP space and have chosen to IP our resources under a `10.0.0.0/16` CIDR. This is a broad IP space and conflicts with the IP space of both the Palo Alto firewall VNET and the CDC intranet.

To ensure we do not have an overlapping IP range, we must be assigned an IP CIDR directly from the CDC. They will create a VNET with this IP CIDR for us and then we can redeploy our resources under this VNET.

### Scope of This Proposal

This proposal is to reach an understanding of the size of the IP CIDR we must request from the CDC for each environment.

This proposal *will not* address how we redeploy our resources to the new VNET nor will it address any Palo Alto firewall setup. Those items will be addressed in future proposals.

## Goals

* Determine an IP CIDR range to request from the CDC per environment-region pair
* The IP CIDR range must meet our current and future needs
* We should request the smallest range possible that will meet our needs
    * If we cannot provide justification to the range size with the CDC, our request may be rejected
* Understand how we will allocate the CIDR to our individual subnets

## IP Usage Today

Our Azure resources are deployed in two different regions to enable regional redundancy in the event of a region failure. Due to this, we will need to request two VNETs per environment.

For each subnet we create, [Azure reserves 5 IP addresses](https://docs.microsoft.com/en-us/azure/virtual-network/virtual-networks-faq#are-there-any-restrictions-on-using-ip-addresses-within-these-subnets). Therefore the smallest CIDR allowed for a subnet is `/29`.

Not all subnets have non-reserved IPs in use. This is because the communication over the subnet takes place under a reserved IP address [using Azure service endpoints](https://docs.microsoft.com/en-us/azure/virtual-network/virtual-network-service-endpoints-overview).

### East US VNET (`10.0.0.0/16`)

Our primary region.

#### Subnet Usage

| Subnet | Description | CIDR | IPs | Used | Available |
|:--|:--|--:|--:|--:|--:|
| public | Internet-routable resources | 10.0.1.0/24 | 256 | 5 + 0 = 5 | 251 |
| container | Docker containers (ex. SFTP test) | 10.0.2.0/24 | 256 | 5 + 2 = 7 | 249 |
| private | VNET-only; not Internet-routable | 10.0.3.0/24 | 256 | 5 + 0 = 5 | 251 |
| GatewaySubnet | Required for a VPN gateway; smallest allowed is `/27` | 10.0.4.0/24 | 256 | 5 + 1 per connection | - |
| endpoint | [Azure Private Endpoints](https://docs.microsoft.com/en-us/azure/private-link/private-endpoint-overview) | 10.0.5.0/24 | 256 | 5 + 8 = 13 | 243 |

#### VNET Summary

| Count | Label |
|--:|:--|
| 65,536 | IPs in CIDR |
| 1280 | Allocated IPs to Subnets |
| 64,256 | Available IPs |

### West US VNET (`10.1.0.0/16`)

Our secondary region. Includes a redundant database.

#### Subnet Usage

| Subnet | Description | CIDR | IPs | Used | Available |
|:--|:--|--:|--:|--:|--:|
| private | VNET-only; not Internet-routable | 10.1.3.0/24 | 256 | 5 + 0 = 5 | 251 |
| endpoint | [Azure Private Endpoints](https://docs.microsoft.com/en-us/azure/private-link/private-endpoint-overview) | 10.1.5.0/24 | 256 | 5 + 1 = 6 | 250 |

#### VNET Summary

| Count | Label |
|--:|:--|
| 65,536 | IPs in CIDR |
| 512 | Allocated IPs to Subnets |
| 65,024 | Available IPs |

## What Will Change with a CDC VNET

Looking toward the future, there will be subnets we will not utilize when we migrate to a CDC VNET:

* **‌GatewaySubnet**
    * We will no longer be able to manage our own VPN, as the Palo Alto firewall will not allow ingress into the VNET
        * Ingress is allowed through CyberArk, which will be adopting in the future
    * We will be able to use the Azure Console, CLI, and Terraform to manage resources, as Azure resources connect both to the VNET and the Internet
        * We will, however, need to manually manage IP rules until CyberArk is deployed
* **endpoint**
    * Since certain Azure resources sever their connection to the internet when a private endpoint is deployed (ex. function app), without a VPN, we will no longer be able to use private endpoints

## Proposed IP CIDR

With the above considerations, the proposed CIDR request per environment/region pair is:

**x.x.x.x/25**

### Why a `/25`?

* A `/25` will grant us 128 IP addresses per VNET
* While we currently use a limited number of IP addresses, we cannot anticipate our future needs
* We may want to utilize private endpoints again in the future
* While our current utilized resources do not consume VNET ip addresses when scaled (ex. function app, database, etc), we may in the future have bare containers that need to be scaled, which will consume an IP per server

### Why request a `/25` in each environment-region pair?

* Environments should be identical
    * When we conduct full-scale load testing in a lower environment, we’ll want the same number of IPs to verify our scaling logic
* Regions should allow for full redundant replication
    * While the only resource currently deployed in our secondary region is a database replica, we’ll want the capability to stand up our entire system in our secondary region
    * This would grant us full region redundancy and/or provide a closer network path for users on the west coast

#### Subnet Usage

| Subnet | Description | CIDR | IPs | Used | Available |
|:--|:--|--:|--:|--:|--:|
| public | Internet-routable resources | x.x.x.x/28 | 16 | 5 + 0 = 5 | 11 |
| container | Docker containers (ex. SFTP test) | x.x.x.x/28 | 16 | 5 + 2 = 7 | 9 |
| private | VNET-only; not Internet-routable | x.x.x.x/28 | 16 | 5 + 0 = 5 | 11 |
| GatewaySubnet | Not used under a CDC VNET |  | |  |  |
| endpoint | Not used under a CDC VNET |  | |  |  |

#### VNET Summary

| Count | Label |
|--:|:--|
| 128 | IPs in CIDR |
| 48 | Allocated IPs to Subnets |
| 80 | Available IPs |

## Discussion Points

* What future IP needs might we have?
* Are we requesting too large of a CIDR?
* Are we requesting too small of a CIDR?
* What is the smallest CIDR we would be comfortable with?
