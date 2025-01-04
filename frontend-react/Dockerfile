# Builder step

# Use version defined in .nvmrc when calling build command
ARG NODE_VERSION=latest
# Need dev version to use corepack
#FROM cgr.dev/chainguard/node:${NODE_VERSION}-dev AS build
FROM node:${NODE_VERSION}-alpine AS builder
WORKDIR /app
# Prep package manager as root
USER root
COPY --chown=node . .
RUN chown node:node ./ && npm install -g corepack
# Run scripts as node
USER node
RUN yarn install --immutable && yarn build:production

# Server step

FROM cgr.dev/chainguard/nginx AS server
COPY ./nginx.conf /etc/nginx/nginx.conf
COPY --from=builder /app/build /usr/share/nginx/html
EXPOSE 8080