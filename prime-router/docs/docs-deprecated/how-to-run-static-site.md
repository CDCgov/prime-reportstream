# How to run the static site

## Introduction
The current static site that lives at [https://reportstream.cdc.gov](https://reportstream.cdc.gov) 
is created via the 11ty tools. If you want to run it locally you can do it by following these steps.

*Note: You will need to have node.js installed on your local machine and in your path*

## Basic Steps
Before doing these steps, make sure you have run a `gradle clean package` and your Docker container is running, otherwise you will encounter errors when the static site makes requests for data.
1. Open a terminal window in the `frontend` folder in the project
2. Run `npm install`
3. Run `npx eleventy --config eleventy.config.js`. This will generate the static files for you based on the liquid templates in the project.
4. Run `npx eleventy --serve --config eleventy.config.js`. This will start the server and an instance of browsersync. You will see a message that looks like this:
```shell
[HPM] Proxy created: /api  -> http://localhost:7071
(node:72516) [DEP0128] DeprecationWarning: Invalid 'main' field in '/Users/maurice/development/USDS/prime-data-hub/frontend/node_modules/emitter-mixin/package.json' of 'y'. Please either fix that or report it to the module author
(Use `node --trace-deprecation ...` to show where the warning was created)
Writing ./dist/unsupported-browser.html from ./src/error-unsupported-browser.html.
Writing ./dist/404.html from ./src/error-404.html.
Writing ./dist/index.html from ./src/index.html.
Writing ./dist/sign-in/index.html from ./src/login.html.
Writing ./dist/terms-of-service/index.html from ./src/terms-of-service.html.
Writing ./dist/report-details/index.html from ./src/data-show.html.
Writing ./dist/documentation/data-retention/index.html from ./src/data-retention.html.
Writing ./dist/daily-data/index.html from ./src/data-index.html.
Writing ./dist/delivery.html from ./src/delivery.html.
Writing ./dist/schema.html from ./src/schema.html.
Writing ./dist/documentation/index.html from ./src/documentation.html.
Writing ./dist/profile.html from ./src/profile.html.
Writing ./dist/organization.html from ./src/organization.html.
Writing ./dist/documentation/security-practices/index.html from ./src/security-faq.html.
Writing ./dist/documentation/elr-integration-guide/index.html from ./src/elr-guide.html.
Writing ./dist/documentation/web-receiver-guide/index.html from ./src/web-receiver-guide.html.
Writing ./dist/facilities.html from ./src/facilities.html.
Copied 21 files / Wrote 17 files in 0.43 seconds (25.3ms each, v0.12.1)
Watching…
[Browsersync] Access URLs:
 ----------------------------
 Local: http://localhost:8091
 ----------------------------
    UI: http://localhost:3001
 ----------------------------
[Browsersync] Serving files from: ./dist
```
5. Take note of the two urls provided. The `UI` url is for browsersync and allows you to control options about how the site is being loaded. The `Local` address is for the site. That's what you want to navigate to in your browser. *Note: Your ports may be different. This could be important later.*
6. Congrats you can now interact with the site.

## Troubleshooting

### I am getting a CORS error when I try to login via Okta
Unfortunately this can happen. Okta is set up to prevent CORS requests by default, but you can set it up to accept requests from your local dev environment. If you're not an Okta admin, reach out to a team lead and they can get you set up.

### I am getting a CORS error for localhost:7071
Our local Docker container is not set up to allow CORS requests from your copy of the static site. Look at the `start_func.sh` script in the project root. In there you will see a line that allows for a localhost connection to make CORS requests. It should look something like this:
```shell
func host start --cors http://localhost:8090 --language-worker -- "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

You can note that the port for the localhost connection is defaulted to `8090`, but yours may be different. Stop your Docker container and change the start_func script to use your port number. Then start your Docker container and you should be able to make requests from the static site at that point.
   