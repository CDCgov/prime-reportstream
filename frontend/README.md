# Web UI Front-end to PRIME Data Hub

For web interface, the PRIME Data Hub project is leveraging a [JAMStack](https://jamstack.org) approach: JavaScript, APIs, Markup.
Static markup and asset hosting allows well-known security configuration best practices.
Using our APIs demonstrates integration functions.

See [../README.md](../README.md) for PRIME Data Hub project details.


## Getting Started

A terminal multiplexer such as [`tmux`](https://en.wikipedia.org/wiki/Tmux),
[`screen`](https://en.wikipedia.org/wiki/GNU_Screen), or
[iTerm2](https://iterm2.com) is suggested for active development.


##### Static website build

```bash
cd ./frontend/
npm ci
npm run build

# static site root built in `frontend/_site`
ls ./_site
```

##### Auto-reloading web server

```bash
cd ./frontend/
npm install
npm run serve
```

## CI/CD Integration

The frontend is integrated into the GitHub actions build workflow by calling
[`./ci_build.bash`](./ci_build.bash). Executable both locally and as part of
the workflow, the script uses a [docker NodeJS image][dkr_node] for a stable
environment to reproducably build the static site assets.  The multi-stage
[`./Dockerfile`](./Dockerfile) also produces a [docker nginx server][dkr_nginx]
embedding the static website.  The [`./ci_validate.bash`](./ci_validate.bash)
performs a cursory validation on the output nginx server image.


 [dkr_node]: https://hub.docker.com/_/node "Docker official image for NodeJS"
 [dkr_nginx]: https://hub.docker.com/_/nginx "Docker official image for Nginx"


## Tech Stack

- [U.S. Web Design System (USWDS)](https://designsystem.digital.gov/components/)
- [Eleventy](https://www.11ty.dev/docs/) is a static site generator
  - similar to [Jekyll](https://jekyllrb.com) using NPM/NodeJS tooling
  - using [liquid templating](https://www.11ty.dev/docs/languages/liquid/) for Jekyll familiarity

### Browser Support

Current focus is on development iteration speed.

Modern browser engines with > 1% use
- Chrome
- Edge (based on Chrome engine)
- FireFox
- iOS Safari
- Safari

#### IE11 not supported

Support and testing for older browsers like IE11 incurs a significant burdens.
In the present, use of sophisticated tooling with a learning curve as well as
testing demands for a wider range of browsers consume development time.
In the future, the burden becomes the aged tooling, the breadth of legacy
knowledge, and supporting the intertia in the testing infrastructure.
Also, decisions tend to ossify around technical constraints.

Is IE11 a platform that _should_ be supported for a new project?  Not after
2021, according to Microsoft product teams, who are dropping support for IE11
in 2021. Those product teams are actively migrating their userbase to modern
web platforms. Microsoft Edge ships with Microsoft Windows, providing an
obvious migration path. Given the clear signal, customer organizations are already migrating.
Combined with mondern browsers providing a more secure client platform, few
positive reasons remain to put effort into supporting the legacy IE11 browser.

- Microsoft is actively migrating users from IE11 to Edge. (See research in thread)
  - [*MS Teams* ended support 2020-11-30][a] 
  - [*MS 365* will stop support 2021-08-17][b] 
  - [*MS Edge Legacy* support stops on 2021-03-09][c] 
  - [Azure DevOps ended support 2020-12-31][d] 
  - [Azure Portal will stop support 2021-03-31][e]

- Also [dropped support for IE11][z]: Zoom, SalesForce, Dropbox, GitHub, Zendesk, Atlassian, Slack, Bootstrap

[a]: https://docs.microsoft.com/en-us/lifecycle/announcements/internet-explorer-11-support-end-dates "Microsoft apps and services to end support for Internet Explorer 11"
[b]: https://docs.microsoft.com/en-us/lifecycle/announcements/m365-ie11-microsoft-edge-legacy "Microsoft 365 apps and services to end support for IE 11"
[c]: https://techcommunity.microsoft.com/t5/microsoft-365-blog/microsoft-365-apps-say-farewell-to-internet-explorer-11-and/ba-p/1591666 "Microsoft 365 apps say farewell to Internet Explorer 11 and Windows 10 sunsets Microsoft Edge Legacy"
[d]: https://www.swyx.io/ie11-eol/ "IE11 Mainstream End Of Life in Oct 2020"
[e]: techcommunity.microsoft.com/t5/windows-it-pro-blog/the-perils-of-using-internet-explorer-as-your-default-browser/ba-p/331732 "The perils of using Internet Explorer as your default browser"
[z]: https://github.com/gabLaroche/death-to-ie11/blob/develop/src/data/websites.js "Products and websites no longer supporting IE11"

