# Reliable* Pull Request Action

> *Only uses built-in GitHub runner commands

[![Test Action](https://github.com/CDCgov/prime-reportstream/.github/workflows/reliable-pull-request--test-action.yml/badge.svg)](https://github.com/CDCgov/prime-reportstream/.github/workflows/reliable-pull-request--test-action.yml)

## Synopsis

1. Create a pull request on a GitHub repository using existing branches.
2. [actions/checkout](https://github.com/actions/checkout) determins the active repo.

## Usage

```yml
jobs:
  create-pr:
    name: Test create PR on ${{ matrix.os }}
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest]
    steps:
      - name: Checkout the repo
        uses: actions/checkout@v4.1.1

      - name: Create Pull Request
        id: create_pr
        uses: CDCgov/prime-reportstream/.github/actions/reliable-pull-request@ae8d0c88126329ee363a35392793d0bc94cb82e7
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          title: 'Automated Pull Request'
          sourceBranch: ${{ github.ref_name }}
          targetBranch: 'main'
          body: 'This is an automated pull request.'
          labels: 'automated,pr'
          assignees: 'octocat'

      - name: Output PR URL
        run: echo "The PR URL is ${{ steps.create_pr.outputs.PRURL }}"
```

## Inputs

```yml
inputs:
  title:
    description: 'Pull Request Title'
    required: true
  sourceBranch:
    description: 'Source Branch Name'
    required: true
  targetBranch:
    description: 'Target Branch Name'
    required: true
  body:
    description: 'Pull Request Body'
    required: false
  labels:
    description: 'Labels (comma-separated)'
    required: false
  assignees:
    description: 'Assignees (comma-separated)'
    required: false
```

## Outputs
```yml
outputs:
  PRURL:
    description: 'The URL of the created pull request'
```

## Requirements

The following permissions must be set for the repository:
  * `Settings > Actions > General`
     * Workflow permissions
       1. Read and write permissions
       2. Allow GitHub Actions to create and approve pull requests
       3. Save

>*Alternative is to set [jobs.<job_id>.permissions](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#jobsjob_idpermissions)*
