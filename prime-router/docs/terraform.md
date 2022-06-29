# Terraform 
We utilize terrform to describe the entire infrastructure for PRIME ReportStream. Any adjustments to the infrastructure should be done here so they are documented, and repeatable.

## Versions

- [Terraform] - 1.0.5


## Azure Prerequisites
We assume the following infrastructure has already been deployed by CMS. 
- Resource Group for underlying infrastructure
- VNETs - Redundant vnets for both East and West US as well as VNETS tied to a VPN.
- VPN - A VPN that will connect to the VPN VNETs
- Storage Account - Used to store the terraform tf state.
- Application Key Vault - Prepopulated with the following secrets
  - _functionapp-postgres-user_: User for the postgresql hosted instance
  - _functionapp-postgres-pass_: Password for the postgresql hosted instance
  - _pagerduty-integration-url_: URL for the pagerduty alerts.
- 

## Layout
The terraform code is layed out in the following folder structure. 
The base folder path is `/operations/app/terraform`
```
operations/app/terraform
|
└─── modules
    │
    └─── <module_name>
    |   │   ~inputs.tf              # Any inputs needed by the module.
    |   │   ~outputs.tf             # Outputs from resources in the module for use by other modules.
    |   │   main.tf                 # The teffarom resources created by the module.
    |   |   data.tf                 # Any data lookups needed by the module (Not used in all modules)
    |
    └─── vars
        |
        └─── <stage>
            |   ~outputs.tf         # Outputs to stdout from terraform.
            |   azure.tf            # Terraform provisioners/backend and azurerm provider.
            |   data.tf             # Data lookups for aforementioned prerequisites
            |   main.tf             # Main file used for running the various resources.
            |   variables.tf        # All variables used in various modules.

```
**Note:** The individual variables and what they are related to should have a description fild in the variable file. Please refer to that description for additional information as it relates to that specific variable.


# Modules

We utilize several custom modules that are as follows

* app_service_plan - Defines the app service plan used for the function apps
* application_insights - Adds application insights to each resource.
* common
  * private_endpoint - Creates private endpoints for whichever service calls it
  * vnet_dns_zones - DNS virtual network links
* container_registry - Location for the build docker containers
* database - Deploy our Postgresql database and replica
* front_door - Spins up and configures Front Door
* function_app - Creates our main function app
* key_vault - Builds our terraform responsible key vaults
* log_analytics_workspace - Add a LAW for all log files for all resources.
* metabase - App service for our metabase
* nat_gateway - Our gateway for external traffic
* network - Gets more detailed data inside our VNETs
* sftp_container - Creates the test SFTP container
* storage - Adds our Storage Accounts for each service
* vnet - Gets top level data about our VNETs

## DNS
For testing in test, you may use the Azure DNS of 168.63.129.16. Typically though, we will want this set to the IP for the CDC DNS server. You can find more information related to that [here]


## Deploying
A lot of work has been done to simplify the deployment of the infrastructure stack. Each folder inside the `vars` directory represents the state you want to add/update/destroy. Before you run any terraform commands, you will need to modify/verify the `variables.tf` file with the correct data. You will also need to authenticate the az command line application using your SU account.
_Example:_
```sh
az login --use-device-code
```
Once your az cli has been authenticated, you can proceed with the terraform commands you wish to run. 


[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job. There is no need to format nicely because it shouldn't be seen. Thanks SO - http://stackoverflow.com/questions/4823468/store-comments-in-markdown-syntax)

   [terraform]: <https://www.terraform.io/downloads>
   [here]: ./dns.md

# Demo Environments

## Quickstart

1. Navigate to `demo` Terraform directory using one of the following methods:
   * `terraform -chdir=operations/app/terraform/vars/demo`
   * `operations/app/terraform/vars/demo terraform`

2. Specify `-var-file` and `-backend-config` from the desired demo directory (demo1, demo2, or demo3)
   * `-var-file=demo1/env.tfvars.json`
   * `-backend-config=demo1/env.tfbackend`

3. Target the `init` Terraform module to `apply` base resources (vnets, key vaults, etc.)
   * `-target=module.init`

4. After base resources are created, run `apply` without a target


## Example Create

### Specify environment & Terraform path
```bash
env=demo2
path='operations/app/terraform/vars/demo'
```

### Run init, apply init module, and full apply
```bash
terraform -chdir=$path init \
-reconfigure \
-var-file=$env/env.tfvars.json \
-backend-config=$env/env.tfbackend

for i in {1..3}; do \
terraform -chdir=$path apply \
-target=module.init \
-var-file=$env/env.tfvars.json \
-auto-approve; \
sleep 30; \
done

echo "init complete"

for i in {1..3}; do \
terraform -chdir=$path apply \
-var-file=$env/env.tfvars.json \
-auto-approve; \
sleep 60; \
done

echo "apply complete"

```

### One-time config after first creation (initialization):
 1. Download VPN client file:
    * Azure VPN Settings > Point-to-site configuration > Download VPN client > OpenVPN directory
 2. Copy `remote` & `verify-x509-name` to `.github/vpn/<env>.ovpn`
 3. Update key vaults in `operations/dnsmasq/config/<env>/hosts.local` with random id in name.

## Example Destroy

### Specify environment & Terraform path
```bash
env=demo2
path='operations/app/terraform/vars/demo'
```

### VPN and networking resources will remain
```bash
for i in {1..3}; do \
terraform -chdir=$path destroy \
-var-file=$env/env.tfvars.json \
-target=module.app_service_plan \
-target=module.application_insights \
-target=module.container_registry \
-target=module.database \
-target=module.function_app \
-target=module.log_analytics_workspace \
-target=module.sftp_container \
-target=module.storage \
-refresh=false \
-auto-approve; \
sleep 60; \
done

```

## Tips
 1. If errors occur during destroy and you need to manually delete and remove from state:
    * `terraform -chdir=operations/app/terraform/vars/demo state rm module.init`
