FROM centos:centos8.3.2011

ARG AZURE_CLI_VERSION

# Install Azure CLI
RUN rpm --import https://packages.microsoft.com/keys/microsoft.asc
RUN echo -e "[azure-cli]\n\
name=Azure CLI\n\
baseurl=https://packages.microsoft.com/yumrepos/azure-cli\n\
enabled=1\n\
gpgcheck=1\n\
gpgkey=https://packages.microsoft.com/keys/microsoft.asc" | tee /etc/yum.repos.d/azure-cli.repo
RUN yum -y install azure-cli-${AZURE_CLI_VERSION}-1.el7

# The following files need a Maj.min.rev specification that matches the values from $TERRAFORM_VERSION:
# - app/src/environments/01-network/main.tf
# - app/src/environments/02-config/main.tf
# - app/src/environments/03-persistent/main.tf
# - app/src/environments/04-app/main.tf
# - app/src/environments/prod/main.tf
ARG TERRAFORM_VERSION

# Install Terraform
RUN yum install -y yum-utils
RUN yum-config-manager --add-repo https://rpm.releases.hashicorp.com/RHEL/hashicorp.repo
RUN yum -y install terraform-${TERRAFORM_VERSION}

# Create directory for terraform
RUN mkdir /app

# Set alias tf
RUN echo -e '#!/bin/bash\nTFCMD="$@" exec /app/src/environments/tf --env $ENVIRONMENT' > /usr/bin/tf && \
    chmod +x /usr/bin/tf

ENTRYPOINT /bin/bash
