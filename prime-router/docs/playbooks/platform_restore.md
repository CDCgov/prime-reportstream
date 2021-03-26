## Conditions

In the event that Azure's East US region becomes unavailable, the entire platform can be restored by configuring terraform to deploy to another Azure region.  

Alternatively, individual resources can be restored within the environment as well.

## Prerequisites

### Full Platform Restore

To configure terraform to deploy to a new Azure region, update the local `location` variable within the `prime_data_hub` module.  For example, changing `location = "eastus"` to `location = "westus"` will update the terraform configuration to deploy to Azure's West US region.

### Partial Restore

If only individual resources are needed to be restored, make any terraform configuration updates (if needed).

## Actions

### Full Platform Restore in Separate Region

1. Update the `prime_data_hub` module within the terraform configuration to deploy to the desired Azure region.
2. Run `terraform plan -out plan.tf` and review the planned configuration changes.
3. Run `terraform apply plan.tf` to make the infrastructure updates.

### Restore Individual Resources

1. Make any necessary configuration updates.  Examples may include a new Azure region or a new resource name.
2. Run `terraform plan -out plan.tf` and review the planned configuration changes.
3. Run `terraform apply plan.tf` to make the infrastructure updates.
4. [OPTIONAL] You can also target a specific resource with the `plan/apply`.  For example, run `terraform plan -target module.prime_data_hub.module.function_app -out plan.tf` to target the function app specifically.
