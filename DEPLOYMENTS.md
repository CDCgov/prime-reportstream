## When do ReportStream deployments take place?

ReportStream production deployments take place on Tuesdays and Thursday around 10am EST.

The testing and staging environments are automatically deployed on every merge into `master` through our Continuous Deployment pipeline.

## How is ReportStream deployed?

We automatically deploy changes through [GitHub Actions](.github/workflows/release.yml) based on changes in target branches as described in the table below. These changes can only enter these branches through successfully reviewed Pull Requests.

| Changes are merged into branch | Changes get deployed into environment(s) |
|:--|:--|
| `master` | test and staging |
| `production` | production |

The deployment process is zero-downtime with a blue/green deployment process used [via Azure Function deployment slots](https://docs.microsoft.com/en-us/azure/azure-functions/functions-deployment-slots). The deployment process is [documented in PR #1318](https://github.com/CDCgov/prime-reportstream/issues/1318).

Our CI/CD pipeline does not run the Smoke Tests; therefor you *must* run the Smoke tests manually after every deployment. While this will change in the near future (i.e. in the future, they _will_ be included in the pipeline), until that time, *you must run the Smoke tests manually and validate they complete successfully* before directing _any_ traffic to the newly deployed code.
You can track #656 for the status on running the Smoke tests automatically.

## What is deployed?

The GitHub Action deploys changes in the following directories:

| Directory | Deployment Process |
|:--|:--|
| [/prime-router](/prime-router) | New version released to Azure Functions |
| [/frontend](/frontend) | Static site served via an Azure Storage Account |
| [/operations](/operations) | Not deployed. (Coming soon in, see #578) |

## What is our deployment process for production?

In preparation for our Tuesday and Thursday deployments, a cutoff time has been established.

| Deployment Window | Cutoff Time |
|:--|:--|
| Tuesday, 10am EST | Monday, 12pm EST |
| Thursday, 10am EST | Wednesday, 12pm EST |

The cutoff time is automatically enforced via automatic branching from `master` into a dedicated deployment branch targeting the `production` branch through a [GitHub Action](.github/workflows/prepare_deployment_branch.yaml).

1. At the specified cut-off time (Mondays and Wednesdays at noon ET), the GitHub action creates a new branch named `deployment/YYYY-MM-DD` (where the YYYY-MM-DD is `today + 1day`, i.e. the date of the deployment, not the date of the branching) which branches from `master`. This branch now contains everything that was present in `master` at that cut-off time. This is the content that is/will be part of the production deployment.
1. A new PR from the deployment branch is filed to merge `deployment/YYYY-MM-DD` into `production`. The PR has title `"Deployment of YYYY-MM-DD"` and is tagged with the [`deployment` tag](https://github.com/CDCgov/prime-reportstream/issues?q=label%3Adeployment).
2. The contents of `master` is deployed to the staging environment for verification
    * Manual testing takes place
3. The PR is reviewed by the team
4. The PR is merged during the specified deployment window by an approved team member.   Note: Special care must be taken if the release includes schema changes that add or remove data elements for receivers whose data is waiting for the `batch` step at the time of deployment.    Inconsistencies between data models and actual internal data can cause errors.  At this time, our solution has been to do the deployments at times when there is no work-in-progress (WIP) in the system.
5. The GitHub Action deploys the PR to the production environment
6. A smoke test is run against production