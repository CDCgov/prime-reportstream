SHELL := bash
.SHELLFLAGS := -eu -o pipefail -c

TF_ENV = "dev"
TF_ENV_MODULE = ""

# HELP
# This will output the help for each task
# thanks to https://marmelab.com/blog/2016/02/29/auto-documented-makefile.html
.PHONY: help

help: ## This help
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z0-9_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.DEFAULT_GOAL := help


# TERRAFORM

build-tf: ## Build the Terraform docker container
	@docker-compose build

tf-cli: build-tf ## Drops into a Docker container configured with the Terraform CLI and Azure
	docker-compose run -e ENVIRONMENT=$(TF_ENV) -w /app/src/environments/$(TF_ENV_MODULE) tf-cli

tf-01-network: ## Preconfigures Terraform network environment module
	${MAKE} tf-cli TF_ENV_MODULE="01-network"

tf-02-config: ## Preconfigures Terraform config environment module
	${MAKE} tf-cli TF_ENV_MODULE="02-config"

tf-03-persistent: ## Preconfigures Terraform persistent environment module
	${MAKE} tf-cli TF_ENV_MODULE="03-persistent"

tf-04-app: ## Preconfigures Terraform app environment module
	${MAKE} tf-cli TF_ENV_MODULE="04-app"