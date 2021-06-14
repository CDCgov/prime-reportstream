SHELL := bash
.SHELLFLAGS := -eu -o pipefail -c

TF_ENV = "dev" #VAR Your target environment (e.g. dev, test, staging, prod)
TF_STAGE = ""  # not surfaced

# HELP
# This will output the help for each task
# thanks to https://marmelab.com/blog/2016/02/29/auto-documented-makefile.html
.PHONY: help tf-cli tf-01-network tf-02-config tf-03-persistent tf-04-app

help: ## This help
	@echo "Invoke any of the following targets"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z0-9_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST) | sed "s/^/    - /g" | sort
	@echo ""
	@echo "When invoking, you can specify the following variables on invocation"
	@awk 'BEGIN {FS = " = .*?#VAR "} /^[a-zA-Z0-9_-]+ = .*?#VAR / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST) | sed "s/^/    - /g" | sort
	@echo ""
	@echo "Example: "
	@echo "    make TF_ENV=\"test\" all"
	@echo ""

.DEFAULT_GOAL := help


# TERRAFORM

build-tf: ## Build the Terraform docker container
	@docker-compose build

tf-cli: build-tf ## Drops into a Docker container configured with the Terraform CLI and Azure
	docker-compose run -e ENVIRONMENT=$(TF_ENV) -w /app/src/environments/$(TF_STAGE) tf-cli

tf-01-network: ## Preconfigures Terraform network environment module
	${MAKE} tf-cli TF_STAGE="01-network"

tf-02-config: ## Preconfigures Terraform config environment module
	${MAKE} tf-cli TF_STAGE="02-config"

tf-03-persistent: ## Preconfigures Terraform persistent environment module
	${MAKE} tf-cli TF_STAGE="03-persistent"

tf-04-app: ## Preconfigures Terraform app environment module
	${MAKE} tf-cli TF_STAGE="04-app"
