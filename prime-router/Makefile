OS_NAME := $(shell uname -m)

.PHONY: help
help: ## Show this help.
	@egrep '^[a-zA-Z_\.%-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

.PHONY: docker-build
docker-build: ## Spin up the docker containers needed to build the application
docker-build:
	docker-compose -f docker-compose.build.yml up -d; \
	sleep 1

build: ## Build the prime-router application
build: src/**/* docker-build
	./gradlew package

.vault/env:
	mkdir .vault/env

.vault/env/.env.local: .vault/env
	touch .vault/env/.env.local

.PHONY: docker
docker: ## Bring up docker containers needed for running the prime router
docker: $(OS_NAME).docker

.PHONY: arm64.docker
arm64.docker: build .vault/env/.env.local
	docker-compose -f docker-compose.yml up -d vault; \
  	while [ ! -s .vault/env/.env.local ]; do sleep 1; done; \
	docker-compose -f docker-compose.build.yml -f docker-compose.yml up -d azurite sftp

.PHONY: x86_64.docker
x86_64.docker: build .vault/env/.env.local
	docker-compose -f docker-compose.yml up -d vault; \
  	while [ ! -s .vault/env/.env.local ]; do sleep 1; done; \
	docker-compose -f docker-compose.build.yml -f docker-compose.yml up -d prime_dev azurite sftp settings

.PHONY: run
run: ## Run the prime router application
run: $(OS_NAME).run

.PHONY: arm64.run
arm64.run: docker setPermissions
	make reloadState & ./gradlew run;

/PHONY: setPermissions
setPermissions: ## Set permissions needed for smoke tests to run successfully
setPermissions: 
	export $$(xargs < .vault/env/.env.local); \
	./prime create-credential --type=UserPass --persist=DEFAULT-SFTP --user foo --pass pass; \
	docker-compose exec sftp chmod 777 /home/foo/upload;

.PHONY: reloadState
reloadState: ## Load the tables and settings into a running prime router
reloadState:
	./gradlew reloadTables; \
	./gradlew reloadSettings;

.PHONY: x86_64.run
x86_64.run: docker setPermissions

.PHONY: restart
restart: ## When running completely in docker; restart the prime router application and docker container
restart: docker
	docker-compose restart prime_dev

.PHONY: clean
clean: ## Stop containers and remove all artifacts
clean:
	docker-compose -f docker-compose.build.yml -f docker-compose.yml down
	sudo rm -rf build
	sudo rm -rf .vault/env
	docker volume prune

.PHONY: cleanGitBranches
cleanGitBranches: ## Cleans up old pruned git branches
cleanGitBranches:
	git fetch -p
	for branch in $(git branch -vv | grep ': gone]' | awk '{print $1}'); do git branch -D $branch; done