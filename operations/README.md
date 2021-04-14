# PRIME Data Hub Operations

The PRIME Data Hub uses Terraform to manage our Azure development environment. All Azure configuration should be done through Terraform to ensure consistency between environments.

To ensure our Terraform state is managed with consistent Terraform versions, we are running Terraform through a Docker image. Terraform should not be used outside of this Docker image to ensure the Terraform core, plugins, and other versions all remain identical.


## Connect to the VPN

All infrastructure operations must be done behind the environment-specific VPN. You can find [directions for configuring your VPN client in prime-router/docs/VPN.md](https://github.com/CDCgov/prime-data-hub/blob/master/prime-router/docs/vpn.md).


## Run Terraform interactively

```
docker-compose run {dev,test,staging,prod}
```

Running the above command will drop you into an interactive bash terminal for the designated environment.


## Login to Azure CLI

```
az login
```

- Navigate to the provided login URL and input the provided token

This only needs to be on first run and after your Azure credentials expire. The state of your login will be persisted in a Docker volume.


## Set the default Azure subscription

If you have access to multiple Azure subscriptions, ensure you set the intended subscription as the default, or you will see errors from Terraform:

```
az account list
az account set {subscription-id}
```


## Using Terraform

Initialize the Terraform environment (this needs to completed on first run and after every new module)

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

Once you are authenticated with Azure CLI, you can access your locally stored terraform state through the bind mount.  If you are developing for the `test`, `staging`, or `prod` environments, the terraform state will be stored in the respective Azure storage account. Within `dev` you will be prompted to enter your developer name (eg. cglodosky) to deploy to the appropriate resource group.


## Changing the Dockerfile

After making any changes to the `Dockerfile` make sure you run:

```
docker-compose build
```


## Deploying a new environment

The order matters for deploying an environment from scratch. While Terraform handles most of the work, the order of operations matter as items like the VPN must be stood up first.

We have not recently deployed an environment from scratch, so we could use assistance in building this documentation.

The rough steps will be:

### Deploy the network and VPN

Use resource targeting to deploy only the network module.

```
terraform plan -target module.prime_data_hub.module.network -out plan.out
terraform apply plan.out
```

### Build a VPN profile

[See PR #638 for directions on standing up a VPN.](https://github.com/CDCgov/prime-data-hub/pull/638)

### Deploy the environment several times

Azure requires multiple Terraform deploys to create the service identities needed for building access profiles. Due to this, the deploy will need to be run several times. The first time a service identity is created, the second time the access profile is created from the identity.

There will also need to be some resource targeting throughout the deployment. This is a section we will need help documenting.