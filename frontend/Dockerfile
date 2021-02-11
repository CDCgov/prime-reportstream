FROM node:14-buster-slim as static_site
WORKDIR /usr/app
COPY package*.json ./
ENV NODE_ENV=production
RUN npm ci
COPY . ./
RUN npm -s run build


# Future: minify assets
#   - maybe using this [minify](https://github.com/tdewolff/minify) Go-based utility
#   - inspired by this [hugo basic example](https://github.com/jtreminio/hugoBasicExample/blob/1e53932c021f0a56f763e3b840533a7894063081/Dockerfile#L16-L35)


FROM nginx:alpine as nginx_host
HEALTHCHECK --interval=60s --timeout=5s --retries=3 \
  CMD /usr/bin/curl -s -f --head http://127.0.0.1/ || exit 1
COPY --from=static_site /usr/app/_site/ /usr/share/nginx/html/

