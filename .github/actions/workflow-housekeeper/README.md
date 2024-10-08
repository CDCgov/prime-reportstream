# Workflow Housekeeper

Retain a time period or quantity of workflow runs.

[![Test action](https://github.com/JosiahSiegel/workflow-housekeeper/actions/workflows/test_action.yml/badge.svg)](https://github.com/JosiahSiegel/workflow-housekeeper/actions/workflows/test_action.yml)

### Dependencies:

>Change in repo: `Settings -> Actions -> General -> Workflow Permissions to allow read and write`

## Inputs
```yml
  ignore-branch-workflows:
    description: 'Ignore runs from workflows currently in ./github/workflow'
    required: false
  retention-time:
    description: 'Period of time to maintain history. E.g. "2 weeks", "3 days", etc.'
    required: false
  retain-run-count:
    description: 'Number of latest runs to keep'
    required: false
  dry-run:
    description: 'Only list runs pending deletion'
    required: false
```

## Usage
```yml
      - name: Checkout
        uses: actions/checkout@v3
      - name: Run workflow housekeeper
        uses: josiahsiegel/workflow-housekeeper@<CURRENT_VERSION>
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```
or
```yml
      - name: Checkout
        uses: actions/checkout@v3
      - name: Run workflow housekeeper
        uses: josiahsiegel/workflow-housekeeper@<CURRENT_VERSION>
        id: scan
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          ignore-branch-workflows: true
          retention-time: '1 days'
          retain-run-count: 1
          dry-run: false
```

## Generated summary
### ✨ Workflow Housekeeper ✨
  * .github/workflows/test_action.yml 4618840926
  * .github/workflows/test_action.yml 4618827035