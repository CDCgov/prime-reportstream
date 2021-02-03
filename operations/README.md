## Build the docker image

```docker build -t terraform .```


## Run the container interactively

```docker run -it --rm -v <path_to_terraform_config>:/scratch --name terraform terraform```


## Login to Azure CLI

```az login```
- Navigate to the provided login URL and input the provided token


## Terraform development

Once you are authenticated with Azure CLI, you can access your locally stored terraform state through the bind mount.  If you are developing for the `test` or `prod` environments, the terraform state will be stored in the respective Azure storage account.  Navigate to the `dev`, `test`, or `prod` directory under `environments` to run a plan for the appropriate environment.  Within `dev` you will be prompted to enter your developer name (eg. cglodosky) to deploy to the appropriate resource group.
