FROM node:alpine as static_site
WORKDIR /usr/app
COPY package*.json ./
ENV NODE_ENV=production
COPY . ./
RUN npm install
RUN npx eleventy --config eleventy.config.js


FROM nginx:latest as nginx_host
COPY --from=static_site /usr/app/_site/ /usr/share/nginx/html/
CMD "'nginx' '-g' 'daemon off;'"
ENTRYPOINT [ "/docker-entrypoint.sh" ]

