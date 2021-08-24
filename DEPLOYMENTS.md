## When do ReportStream deployments take place?

ReportStream production deployments take place on Tuesdays and Thursday around 10am EST.

The lower environments, test and staging, are automatically deployed on every merge to `master` (a.k.a. continuous deployment).

## How is ReportStream deployed?

ReportStream is deployed via [this GitHub Action](.github/workflows/release.yml).

The GitHub Action automatically deploys changes in the following branches to the specified environments:

| Branch | Environment |
|:--|:--|
| `master` | test, staging |
| `production` | production |

Code merged to the above branches always go through a PR review.

The deployment process is zero-downtime with a blue/green deployment process used [via Azure Function deployment slots](https://docs.microsoft.com/en-us/azure/azure-functions/functions-deployment-slots). The deployment process is documented in PR #1318.

Smoke tests are run manually after a deployment, but in the near future the will run automatically before directing traffic to the newly deployed code (work tracked in #656).

## What is deployed?

The GitHub Action deploys changes in the following directories:

| Directory | Deployment Process |
|:--|:--|
| [/prime-router](/prime-router) | New version released to Azure Functions |
| [/frontend](/frontend) | Static site served via an Azure Storage Account |
| [/operations](/operations) | Not deployed. Coming soon in #578 |

## What is our deployment process for production?

In preparation for our Tuesday and Thursday deployments, a cutoff time has been established.

| Deployment Window | Cutoff Time |
|:--|:--|
| Tuesday, 10am EST | Monday, 12pm EST |
| Thursday, 10am EST | Wednesday, 12pm EST |

The cutoff time is automatically enforced via [this GitHub Action](.github/workflows/prepare_deployment_branch.yaml).

1. At the specified cutoff a time, a PR is created to merge the contents of `master` to the `production` branch
2. The contents of `master` is deployed to the staging environment for verification
    * Manual testing takes place
3. The PR is reviewed by the team
4. The PR is merged during the specified deployment window by an approved team member
5. The GitHub Action deploys the PR to the production environment
6. A smoke test is run against production