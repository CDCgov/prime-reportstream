## Platform Restore to Different Region

In the event that Azure's East US region becomes unavailable, the entire platform can be restored by configuring
Terraform to deploy to another Azure region.

### Actions to Mitigate 

To configure Terraform to deploy to a new Azure region, update the local `location` in the environment's `variables.tf`
file -- for example, in `operations/app/terraform/vars/production.tf`.  If you're restoring the platform to Azure's West
US region you'd change:

```
location = "eastus"
```

to:

```
location = "westus"
```

1. Follow the directions in the [operations README](https://github.com/CDCgov/prime-reportstream/tree/master/operations)
   to prepare the region for use by Terraform. This may include creating a storage account, populating Key Vaults, and
   other actions.
2. Update the relevant environment's module as specified above in the Terraform configuration to deploy to the desired
   Azure region.
3. Run `terraform plan -out plan.tf` and review the planned configuration changes.
4. Run `terraform apply plan.tf` to make the infrastructure updates.
