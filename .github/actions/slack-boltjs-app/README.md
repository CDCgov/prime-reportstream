# Slack-Boltjs-App

[![CodeQL](https://github.com/CDCgov/prime-reportstream/.github/workflows/slack-boltjs-app--codeql.yml/badge.svg)](https://github.com/JosiahSiegel/slack-bolt/actions/workflows/codeql.yml)
[![njsscan sarif](https://github.com/CDCgov/prime-reportstream/.github/workflows/slack-boltjs-app--njsscan.yml/badge.svg)](https://github.com/JosiahSiegel/slack-bolt/actions/workflows/njsscan.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/3f3ee15eaf3f4c81915d658008e01c3f)](https://app.codacy.com/gh/CDCgov/prime-reportstream/.github/actions/slack-boltjs-app/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

![](https://avatars.slack-edge.com/2023-04-24/5159910288243_7af56ae264408f296381_128.png)

<a href="https://glitch.com/edit/#!/remix/slack-boltjs-app"><img alt="Remix on Glitch" src="https://cdn.gomix.com/f3620a78-0ad3-4f81-a271-c8a4faa20f86%2Fremix-button.svg"></a>

## A secure and simple Boltjs app for Slack ChatOps

> App includes basic GitHub push functionality to get you started

### Quickstart
* `cp .env.example .env`
  * Update `.env` ⚠️(required)⚠️
* `cp .help.example .help`
  * Update `.help` (optional)
* `npm install`
* `npm start`

### Helpful links

* [Bolt getting started guide](https://api.slack.com/start/building/bolt)
* [Bolt documentation](https://slack.dev/bolt)
* [Slack app home](https://api.slack.com/apps)

### Example Slack app manifest

```yml
display_information:
  name: Bolt DevBot
features:
  app_home:
    home_tab_enabled: true
    messages_tab_enabled: false
    messages_tab_read_only_enabled: false
  bot_user:
    display_name: DevBot
    always_online: true
oauth_config:
  scopes:
    bot:
      - app_mentions:read
      - calls:write
      - channels:read
      - chat:write
      - commands
      - channels:history
      - reactions:read
settings:
  event_subscriptions:
    bot_events:
      - app_home_opened
      - app_mention
      - message.channels
      - reaction_added
  interactivity:
    is_enabled: true
  org_deploy_enabled: false
  socket_mode_enabled: true
  token_rotation_enabled: false
```

Tips:

* For [external workspace users][1], add an `app.message` per `app.command`.
* Check vulnerabilities: `npm audit`
* Fix Glitch out of sync with repo:
  * `git pull`
  * `refresh`
* Hard refresh Glitch from repo:
  * `git fetch --all`
  * `git reset --hard origin/main`
  * `refresh`

---

[Glitch](https://glitch.com/~slack-boltjs-app)
[GitHub](https://github.com/CDCgov/prime-reportstream/.github/actions/slack-boltjs-app)

[1]: https://slack.com/help/articles/115004151203-Slack-Connect-guide--work-with-external-organizations
