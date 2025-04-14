# Start with the latest version of hardened builder image
FROM cgr.dev/chainguard/wolfi-base:latest AS builder

# Install required dependencies
RUN apk add --no-cache bash curl git ca-certificates libstdc++ coreutils && \
    update-ca-certificates && \
    touch ~/.bash_profile

# Get desired Node.js version and install it
COPY .nvmrc /tmp/.nvmrc
RUN export NODE_VERSION=$(cat /tmp/.nvmrc | tr -d '[:space:]') && \
    ARCH=$(uname -m) && \
    echo $ARCH && \
    case $ARCH in \
        x86_64) ARCH_NAME="x64";; \
        aarch64) ARCH_NAME="arm64";; \
        *) echo "Unsupported architecture: $ARCH" && exit 1;; \
    esac && \
    echo "Architecture: $ARCH_NAME" && \
    PLATFORM_ARCH="linux-${ARCH_NAME}" && \
    echo "Platform architecture: $PLATFORM_ARCH https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-${PLATFORM_ARCH}.tar.gz" && \
    echo "Installing Node.js version: ${NODE_VERSION} for $ARCH_NAME" && \
    DOWNLOAD_URL="https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-${PLATFORM_ARCH}.tar.gz" && \
    echo "Downloading from: $DOWNLOAD_URL" && \
    curl -fsSL --retry 3 "$DOWNLOAD_URL" -o /tmp/node.tar.gz && \
    tar -xzf /tmp/node.tar.gz -C /usr/local --strip-components=1 && \
    rm /tmp/node.tar.gz /tmp/.nvmrc && \
    echo -n "Node.js installed version: " && node -v && \
    echo -n "npm installed version: " && npm -v && \
    apk del glibc # Remove glibc package to resolve CVE CVE-2025-0395


# Install yarn and resolve vulnerability in cross-spawn, by upgrading it to a version with resolved CVE
# Newly found CVEs can be resolved in similar manner - by upgrading to the closest fixed version
RUN apk add --no-cache yarn && \
    npm install -g cross-spawn@7.0.3
# Extract Node.js version from the  image
SHELL ["/bin/ash", "-o", "pipefail", "-c"]
RUN node --version | awk -F'v' '{print $2}' 
WORKDIR /app
# Prep package manager as root and drop privileges
USER root
COPY --chown=nonroot . .
RUN chown nonroot:nonroot ./ && npm install -g corepack
# Run install/buiuld as unprivileged user
USER nonroot
RUN yarn install --immutable && yarn build:production

# Web server stage
# This image runs as a unprivileged user by default, so there's no need to explicitly set user - see the Note block in the link below for more context
# https://edu.chainguard.dev/chainguard/chainguard-images/getting-started/nginx/#advanced-usage
FROM cgr.dev/chainguard/nginx AS server
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=builder /app/build /usr/share/nginx/html
EXPOSE 8080
CMD ["-g", "daemon off;"]
