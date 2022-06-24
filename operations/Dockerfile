ARG TERRAFORM_VERSION
FROM hashicorp/terraform:${TERRAFORM_VERSION}

ARG AZURE_CLI_VERSION
RUN \
  apk update && \
  apk add bash py-pip git && \
  apk add --virtual=build gcc libffi-dev musl-dev openssl-dev python3-dev make && \
  pip install azure-cli==${AZURE_CLI_VERSION} && \
  apk del --purge build 

# The following files need a Maj.min.rev specification that matches the values from $TERRAFORM_VERSION:
# - app/src/environments/01-network/main.tf
# - app/src/environments/02-config/main.tf
# - app/src/environments/03-persistent/main.tf
# - app/src/environments/04-app/main.tf
# - app/src/environments/prod/main.tf

# Create directory for terraform
RUN mkdir /app

# Set alias tf
RUN echo -e '#!/bin/bash\nTFCMD="$@" exec /app/src/environments/tf --env $ENVIRONMENT' > /usr/bin/tf && \
    chmod +x /usr/bin/tf

ENTRYPOINT /bin/bash
