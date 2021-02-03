# PRIME Data Hub Operations

The PRIME Data Hub uses Terraform to manage our Azure development environment. All Azure configuration should be done through Terraform to ensure consistency between environments.

To ensure our Terraform state is managed with consistent Terraform versions, we are running Terraform through a Docker image. Terraform should not be used outside of this Docker image to ensure the Terraform core, plugins, and other versions all remain identical.


## Run Terraform interactively

```docker-compose run {dev,test,prod}```

Running the above command will drop you into an interactive bash terminal for the designated environment.


## Login to Azure CLI

```az login```
- Navigate to the provided login URL and input the provided token

This only needs to be on first run and after your Azure credentials expire. The state of your login will be persisted in a Docker volume.


## Using Terraform

Initialize the Terraform environment (only needed once, but doesn't hurt to run each time)
```
terraform init
```

Generate a plan (use the  `-out` flag to ensure the same plan gets applied in the following step)
```
terraform plan -out plan.out
```

Review the above plan, then apply it to the environment
```
terraform apply plan.out
```


## Terraform development

Once you are authenticated with Azure CLI, you can access your locally stored terraform state through the bind mount.  If you are developing for the `test` or `prod` environments, the terraform state will be stored in the respective Azure storage account. Within `dev` you will be prompted to enter your developer name (eg. cglodosky) to deploy to the appropriate resource group.
