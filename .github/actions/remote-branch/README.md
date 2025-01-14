# Remote Branch Action
## Synopsis

1. Create a branch on a remote repository.
2. [actions/checkout](https://github.com/actions/checkout) determins the active repo.

## Usage

### Single repo
```yml
jobs:
  create-branch-action:
    name: Create branch
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repo
        uses: actions/checkout@v4

      - name: Create branch
        uses: CDCgov/prime-reportstream/.github/actions/remote-branch@dbe7a2138eb064fbfdb980abee918091a7501fbe
        with:
          branch: new-branch
```
### Single alternative repo
```yml
jobs:
  create-branch-action:
    name: Create branch
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repo
        uses: actions/checkout@v4

      - name: Checkout alt repo
        uses: actions/checkout@v4
        with:
          sparse-checkout: .
          repository: me/alt-repo
          token: ${{ secrets.ALT_REPO_TOKEN }}
          path: alt-repo

      - name: Create branch on alt repo
        uses: CDCgov/prime-reportstream/.github/actions/remote-branch@dbe7a2138eb064fbfdb980abee918091a7501fbe
        with:
          branch: new-branch-alt-repo
          path: alt-repo
```
### Multiple repos
```yml
jobs:
  create-branch-action:
    name: Create branch
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repo
        uses: actions/checkout@v4

      - name: Checkout second repo
        uses: actions/checkout@v4
        with:
          sparse-checkout: .
          repository: me/second-repo
          token: ${{ secrets.SECONDARY_REPO_TOKEN }}
          path: second-repo

      - name: Create branch
        id: create-branch-action
        uses: CDCgov/prime-reportstream/.github/actions/remote-branch@dbe7a2138eb064fbfdb980abee918091a7501fbe
        with:
          branch: new-branch

      - name: Create branch on second repo
        id: create-branch-action-second-repo
        uses: CDCgov/prime-reportstream/.github/actions/remote-branch@dbe7a2138eb064fbfdb980abee918091a7501fbe
        with:
          branch: new-branch-second-repo
          path: second-repo

      - name: Get create branch status
        run: echo ${{ steps.create-branch-action.outputs.create-status }}

      - name: Get create branch status on second repo
        run: echo ${{ steps.create-branch-action-second-repo.outputs.create-status }}
```

## Inputs

```yml
inputs:
  branch:
    description: Branch name
    required: true
  path:
    description: Relative path under $GITHUB_WORKSPACE to place the repository
    required: false
    default: '.'
```

## Outputs
```yml
outputs:
  create-status:
    description: Branch creation status
    value: ${{ steps.create-branch.outputs.create_status }}
```
