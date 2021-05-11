# Makefile for az cli shortcuts for ReportStream operations

SHELL:=/bin/bash
SOURCE_SLOT?=staging
TARGET_SLOT?=production
DEPLOYED_COMMIT?=$(shell git show --abbrev=7 -s --pretty=%h)

default:
	@echo "Hello, ReportStream operator!"
	@echo "You can use 'make promote-ENV' to promote the staging slot of any environment, if you are logged in to azure"

# Internal target: check if we are currently logged in, so we get a friendly error if not
.be-logged-in:
	@if ! az account show >& /dev/null ; then echo "You must be logged in to the az command line"; exit 1; fi

# Internal target: check if the passed-in wildcard is a known environment name. Hard-coding them because let's be real here.
.valid-env-%:
	@case $* in staging|prod) ;; *) echo "$* is not a valid environment"; exit 1;; esac

promote-%: .be-logged-in .valid-env-%
	az functionapp deployment slot swap -g prime-data-hub-$* -n pdh$*-functionapp --slot $(SOURCE_SLOT) --target-slot $(TARGET_SLOT)