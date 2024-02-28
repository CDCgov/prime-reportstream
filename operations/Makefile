SHELL := bash
.SHELLFLAGS := -eu -o pipefail -c

# Pretty colors
COLOR_AZURE = \033[1;44m
COLOR_NONE = \033[0m

TF_ENV = "dev" #VAR Your target environment (e.g. dev, test, staging, prod)
TF_STAGE = ""  # not surfaced
TF_CMD = ""  # not surfaced

AZ_RESOURCE_PREFIX = "pdh$(TF_ENV)" ## VAR Azure environment resource prefix (e.g. pdhtest)
AZ_RESOURCE_GROUP = "prime-data-hub-$(TF_ENV)" ## VAR Azure resource group (e.g. prime-data-hub-staging)

AZ_SERVICE_PRINCIPAL =  #VAR JSON string containing service credential values provided by Azure. Be careful with escaping quotes when using this variable

ZDD_SOURCE_SLOT = "candidate" #VAR Source function app slot for ZDD deployment
ZDD_TARGET_SLOT = "production" #VAR Destination function app slot to deploy to

# HELP
# This will output the help for each task
# thanks to https://marmelab.com/blog/2016/02/29/auto-documented-makefile.html
.PHONY: help build-tf-cli pull-tf-cli tf-cli tf-cmd tf-01-network tf-02-config tf-03-persistent tf-04-app tf-validate tf-plan zdd-promote-slot

help: ## This help
	@echo "Invoke any of the following targets"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z0-9_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST) | sed "s/^/    - /g" | sort
	@echo ""
	@echo "When invoking, you can specify the following variables on invocation"
	@awk 'BEGIN {FS = " = .*?#VAR "} /^[a-zA-Z0-9_-]+ = .*?#VAR / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST) | sed "s/^/    - /g" | sort
	@echo ""
	@echo "Example: "
	@echo "    make TF_ENV='test' tf-01-network"
	@echo "    make TF_ENV='staging' AZ_RESOURCE_PREFIX='pdhtest' AZ_RESOURCE_GROUP='prime-data-hub-test' zdd-promote-slot"
	@echo "    make TF_ENV='staging' tf-validate"
	@echo "    make TF_ENV='staging' AZ_SERVICE_PRINCIPAL=\"{...service principal json...}\" tf-plan"
	@echo ""

.DEFAULT_GOAL := help


# TERRAFORM

build-tf-cli: ## Build the Terraform docker container
	@docker-compose build

pull-tf-cli: ## Pull the Terraform docker container
	@docker-compose pull || ${MAKE} build-tf-cli

tf-cli: pull-tf-cli ## Drops into a Docker container configured with the Terraform CLI and Azure
	@docker-compose run -e ENVIRONMENT=$(TF_ENV) -w /app/src/environments/$(TF_STAGE) tf-cli

tf-cmd: pull-tf-cli .az-configure-service-principal ## Run a command in the Docker container configured with the Terraform CLI and Azure
	@echo -e ""
	@echo -e ""
	@echo -e "$(COLOR_AZURE)---$(COLOR_NONE)"
	@echo -e "$(COLOR_AZURE)RUNNING COMMAND \"$(TF_CMD)\" AGAINST TERRAFORM STAGE \"$(TF_STAGE)\" IN ENVIRONMENT \"$(TF_ENV)\"$(COLOR_NONE)"
	@echo -e "$(COLOR_AZURE)---$(COLOR_NONE)"
	@echo -e ""
ifdef AZ_SERVICE_PRINCIPAL
	@docker-compose run \
		-e ARM_CLIENT_ID='$(ARM_CLIENT_ID)' \
		-e ARM_CLIENT_SECRET='$(ARM_CLIENT_SECRET)' \
		-e ARM_SUBSCRIPTION_ID='$(ARM_SUBSCRIPTION_ID)' \
		-e ARM_TENANT_ID='$(ARM_TENANT_ID)' \
		-e ENVIRONMENT=$(TF_ENV) \
		-w /app/src/environments/$(TF_STAGE) \
		--entrypoint '${TF_CMD}' tf-cli
else
	@docker-compose run \
		-e ENVIRONMENT=$(TF_ENV) \
		-w /app/src/environments/$(TF_STAGE) \
		--entrypoint "${TF_CMD}" tf-cli
endif

tf-01-network: ## Preconfigures Terraform network environment module
	${MAKE} tf-cli TF_STAGE="01-network"

tf-02-config: ## Preconfigures Terraform config environment module
	${MAKE} tf-cli TF_STAGE="02-config"

tf-03-persistent: ## Preconfigures Terraform persistent environment module
	${MAKE} tf-cli TF_STAGE="03-persistent"

tf-04-app: ## Preconfigures Terraform app environment module
	${MAKE} tf-cli TF_STAGE="04-app"

tf-validate: ## Validates the Terraform configuration across all modules
	${MAKE} tf-cmd TF_STAGE="01-network" TF_CMD="tf validate"
	${MAKE} tf-cmd TF_STAGE="02-config" TF_CMD="tf validate"
	${MAKE} tf-cmd TF_STAGE="03-persistent" TF_CMD="tf validate"
	${MAKE} tf-cmd TF_STAGE="04-app" TF_CMD="tf validate"

tf-plan: ## Outputs the text plan for all modules in /app/plan
	${MAKE} tf-cmd TF_STAGE="01-network" TF_CMD="tf plan-file"
	${MAKE} tf-cmd TF_STAGE="02-config" TF_CMD="tf plan-file"
	${MAKE} tf-cmd TF_STAGE="03-persistent" TF_CMD="tf plan-file"
	${MAKE} tf-cmd TF_STAGE="04-app" TF_CMD="tf plan-file"


# AZURE

# Internal target: set azure service principal values
.az-configure-service-principal:
ifdef AZ_SERVICE_PRINCIPAL
ARM_CLIENT_ID := $(shell echo '$(AZ_SERVICE_PRINCIPAL)' | python3 -c "import sys, json; print(json.load(sys.stdin)['clientId'])")
ARM_CLIENT_SECRET := $(shell echo '$(AZ_SERVICE_PRINCIPAL)' | python3 -c "import sys, json; print(json.load(sys.stdin)['clientSecret'])")
ARM_SUBSCRIPTION_ID := $(shell echo '$(AZ_SERVICE_PRINCIPAL)' | python3 -c "import sys, json; print(json.load(sys.stdin)['subscriptionId'])")
ARM_TENANT_ID := $(shell echo '$(AZ_SERVICE_PRINCIPAL)' | python3 -c "import sys, json; print(json.load(sys.stdin)['tenantId'])")
else
	$(warning No service principal configured. You will be required to login via the container.)
endif

# Internal target: check if we are currently logged in, so we get a friendly error if not
.az-logged-in:
	@if ! az account show >& /dev/null ; then echo "You must be logged in to the az command line"; exit 1; fi

# Internal target: check if the passed-in wildcard is a known environment name. Hard-coding them because let's be real here.
.az-valid-env:
	@case $(TF_ENV) in demo1|demo2|demo3|rheft|test|staging|prod) ;; *) echo "$* is not a valid environment"; exit 1;; esac

zdd-promote-slot: .az-logged-in .az-valid-env ## Promote the function app through a ZDD slot swap
	az functionapp deployment slot swap -g $(AZ_RESOURCE_GROUP) -n $(AZ_RESOURCE_PREFIX)-functionapp --slot $(ZDD_SOURCE_SLOT) --target-slot $(ZDD_TARGET_SLOT)