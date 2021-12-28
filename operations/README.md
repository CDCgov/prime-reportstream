# PRIME ReportStream Operations

PRIME ReportStream uses Terraform to manage our Azure development environment. All Azure configuration should be done through Terraform to ensure consistency between environments.

To ensure our Terraform state is managed with consistent Terraform versions, we are running Terraform through a Docker image. Terraform should not be used outside of this Docker image to ensure the Terraform core, plugins, and other versions all remain identical.

---
# Prerequisites
## Needed software
> Terraform >= [1.0.5](https://www.terraform.io/downloads)

> [Azure cli](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)

**Note**
All CDC Azure infrastructure operations must be done behind the environment-specific VPN. You can find [directions for configuring your VPN client in prime-router/docs/VPN.md](https://github.com/CDCgov/prime-data-hub/blob/master/prime-router/docs/vpn.md).


## Terraform

For production deploys, always deploy from the `master` branch.

Our Terraform code is broken down into two main folders, vars and modules. The vars direcory will contain all the variables needed for the stage you want to deploy to. All variables required to deploy that specific stage should be contained in it's respective folder. This makes it easy to determine where variables need to be changed.

## Common commands
Below are the following most common terraform commands:
- `terraform output` - This will give you the current output of the terraform deploy if there is any.
- `terraform plan` - This will tell you what will be added/removed during a deploy
- `terraform apply` - This will actually deploy and changes you have made.


### Directory Structure

```
terraform
│   README.md
│       
│
└─── vars
│   │
│   └─── dev
│       │   azure.tf
│       │   main.tf
│       │   ...
│   
└─── modules
│   └─── app_service_plan
│   └─── application_insights
│   └─── ...
```

# Modules

We utilize several custom modules that are as follows

* app_service_plan -  
* application_insights - 
* common  
  * private_endpoint - 
  * vnet_dns_zones - 
* container_registry - 
* database - 
* front-door - 
* function_app -
* key_vault -
* metabase -
* nat_gateway -
* sftp_container -
* storage - 
* vnet - 


### Important vars files
This is a list of the important files that are necessary in each stage vars directory.  
```
azure.tf - Terraform specific requirements including backend.
main.tf - Main terraform code page that cals individual modules.
secrets.tf - Defines where secrets are to be pulled from in azure keyvault
variables.tf - Has all necessary variables to run main.tf
```

## Azure Cli
You will need to run the following the first time, or when your token expires.
```
az login
```

- Navigate to the provided login URL and input the provided token

## Using Terraform

The vars directory is meant to contain all you need in order to run the deployment for the specific stage you wish to deploy. 
For example, if you wish to deploy to stage you would do the following steps

- Navigate to the `vars/dev` directory
- Verify all variables in each .tf file are correct.
- Run the following code snippet.





# Deploying a new CDC environment

* Environments must have a unique resource group.
* Resources groups are managed by the CDC.
* Contact the IAM team at <adhelpdsk@cdc.gov> to get your resource group created.

## Create the Terraform storage account

1. Login to your Azure console.
2. In the intended resource group, create a storage account named: `{prefix}terraform`
    * Note: Storage account names do not allow punctuation
3. Follow the screenshots and replicate the settings

![Storage Account Page 1](readme-assets/storage-account-page-1.png)

![Storage Account Page 2](readme-assets/storage-account-page-2.png)

![Storage Account Page 3](readme-assets/storage-account-page-3.png)

![Storage Account Page 4](readme-assets/storage-account-page-4.png)

## Create the Terraform storage container

In your newly created storage account, create a container named: `terraformstate`

![Storage Account Page 5](readme-assets/storage-account-page-5.png)

## Create your Terraform configuration

1. In the `app/src/environments/configuration` folder, create `{env}.tfvars` and `{env}.tfbackend` files
2. The `{env}.tfvars` contains all the environment-specific variables
    * You can base this off an existing environment, if necessary
3. The `{env}.tfbackend` contains the location where Terraform will store your state
    * You can base this off an existing environment and populate with the storage account created above

## Deploy the environment

Our Terraform modules are broken out into four stages, each with the dependencies on the previous layers:

* `01-network`
    * Contains the core network infrastructure
    * VNET, subnets, VPN Gateway, etc.
* `02-config`
    * Contain the infrastructure required for configuring the environment
    * Key Vaults, App Service Plan, etc.
    * After deploying this layer, manual configuration is required
    * This layer is dependent on the network infrastructure being deployed
* `03-persistent`
    * Contains the data storage infrastructure for the application
    * Database, Storage Account
    * This layer is dependent on the configuration infrastructure being deployed and secrets populated
* `04-app`
    * Contain the application servers and related infrastructure
    * Function App, Front Door, Application Insights, etc.
    * The layer is dependent on the persistent layer being deployed
    
To deploy the full state follow the deployment directions at the top of this document in the following order:

1. Deploy `01-network`
2. Create a VPN profile
    * [See PR #638 for directions on standing up a VPN](https://github.com/CDCgov/prime-data-hub/pull/638)
3. Connect to the VPN
    * [Directions for configuring your VPN client in prime-router/docs/VPN.md](https://github.com/CDCgov/prime-data-hub/blob/master/prime-router/docs/vpn.md)
4. Deploy `02-config`
5. Populate the following secrets in the Key Vaults:
   * `{env}-appconfig`:
     * `functionapp-postgres-user`
     * `functionapp-postgres-pass`
   * `{env}-keyvault`:
       * `hhsprotect-ip-ingress`
       * `pagerduty-integration-url`
6. Deploy `03-persistent`
7. Deploy `04-app`

# Tear down a environment

Destroy the Terraform stages in reverse stage order:

```shell
make TF_ENV={dev,test,staging,prod} tf-04-app
tf destroy
exit

make TF_ENV={dev,test,staging,prod} tf-03-persistent
tf destroy
exit

make TF_ENV={dev,test,staging,prod} tf-02-config
tf destroy
exit

make TF_ENV={dev,test,staging,prod} tf-01-network
tf destroy
exit
```

**WARNING:** Removing stages beyond `04-app` will release resources that are irrecoverable. Data is preserved by Azure retention policies, but public IP addresses and other static resources may be released.

Any resource that is not ephemeral is marked with a `lifecycle { prevent_destroy = true }` annotation, requiring manual intervention to tear down the resource.

# Known issues

1. A few `terraform import` commands for azurerm have known to fail unless the resource path is quoted.
Since `./tf.sh -c ""` expects input within double quotes, use single quotes instead: `./tf -e ${ENV_NAME} -c "import a 'b'"` (note `'b'`).

   * Shown in [Terraform docs](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/monitor_diagnostic_setting#import) to address the reported [issue](https://github.com/hashicorp/terraform-provider-azurerm/issues/9347).

2.  If `tf plan` or `tf plan-file` fail with an IP or `404` error *(found in `*.tfplan.stderr`)*, then Azure likely failed to respond and re-running should resolve the issue.
