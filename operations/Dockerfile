FROM centos:centos8.3.2011

ENV AZURE_CLI_VERSION=2.18.0

# Install Azure CLI
RUN rpm --import https://packages.microsoft.com/keys/microsoft.asc
RUN echo -e "[azure-cli]\n\
name=Azure CLI\n\
baseurl=https://packages.microsoft.com/yumrepos/azure-cli\n\
enabled=1\n\
gpgcheck=1\n\
gpgkey=https://packages.microsoft.com/keys/microsoft.asc" | tee /etc/yum.repos.d/azure-cli.repo
RUN yum -y install azure-cli-${AZURE_CLI_VERSION}-1.el7

# Changing the terraform version must also be done in:
# - app/src/environments/dev/main.tf
# - app/src/environments/test/main.tf
# - app/src/environments/staging/main.tf
# - app/src/environments/prod/main.tf
ENV TERRAFORM_VERSION=0.14.5

# Install Terraform
RUN yum install -y yum-utils
RUN yum-config-manager --add-repo https://rpm.releases.hashicorp.com/RHEL/hashicorp.repo
RUN yum -y install terraform-${TERRAFORM_VERSION}-1

# Create directory for terraform
RUN mkdir /app

# Set alias tf
RUN echo -e '#!/bin/bash\nTFCMD="$@" exec /app/src/environments/tf --env $ENVIRONMENT' > /usr/bin/tf && \
    chmod +x /usr/bin/tf

ENTRYPOINT /bin/bash
