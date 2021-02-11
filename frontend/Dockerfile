FROM node:14-buster-slim as build
WORKDIR /usr/app
COPY package*.json ./
ENV NODE_ENV=production
RUN npm ci
COPY . ./
RUN npm -s run build
