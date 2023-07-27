# ChatOps Provisioning

## Application

[slack-boltjs-app](https://github.com/JosiahSiegel/slack-boltjs-app) is installed as a submodule under [operations/slack-boltjs-app](../../operations/slack-boltjs-app/).

To update the submodule, navigate to the subdirectory and `git pull`.

## Deployment

[release_chatops_app.yml](../../.github/workflows/release_chatops_app.yml) builds the container and pushes the image to ACR only when the submodule changes.

When the image changes in ACR, the `pdh<env>-chatops` container instance updates.

## Test

Validate the ChatOps application is working by typing `@DevBot :wave:` in any channel the ChatOps bot exists.

## Environment Variables

 * SLACK_BOT_TOKEN
   * SECRET
   * Stored as `chatops-slack-bot-token`
 * SLACK_APP_TOKEN
   * SECRET
   * Stored as `chatops-slack-app-token`
 * GITHUB_TOKEN
   * SECRET
   * Stored as `chatops-github-token`
 * GITHUB_REPO
   * Default repo to target for GitHub operations
   * Terraform local variable
 * GITHUB_TARGET_BRANCHES
   * Branches permitted as deploy/force-push targets.
   * Terraform local variable
