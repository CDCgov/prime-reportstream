FROM centos:centos8.3.2011

# Install Azure CLI
RUN rpm --import https://packages.microsoft.com/keys/microsoft.asc
RUN echo -e "[azure-cli]\n\
name=Azure CLI\n\
baseurl=https://packages.microsoft.com/yumrepos/azure-cli\n\
enabled=1\n\
gpgcheck=1\n\
gpgkey=https://packages.microsoft.com/keys/microsoft.asc" | tee /etc/yum.repos.d/azure-cli.repo
RUN yum -y install azure-cli-2.18.0-1.el7

# Install Terraform
RUN yum install -y yum-utils
RUN yum-config-manager --add-repo https://rpm.releases.hashicorp.com/RHEL/hashicorp.repo
RUN yum -y install terraform-0.14.4-1

# Create directory for terraform
RUN mkdir /app

ENTRYPOINT /bin/bash
