# Terraform Stats
## Synopsis

Output the following statistics for the Terraform environment:
1. Terraform version
2. Drift count
   * "Drift" refers to changes made outside of Terraform and does not necessary match any resources listed for changes.
3. Resource drifts
4. Change count
   * "Change" refers to change actions that Terraform plans to use to move from the prior state to a new state.
5. Change percent
   * Percentage of changes to total resources.
6. Resource changes

## Usage

```yml
- name: Terraform stats
  uses: josiahsiegel/terraform-stats@<latest-version>
  id: stats
  with:
    terraform-directory: ${{ env.tf-dir }}
    terraform-version: 1.1.9
- name: Get outputs
  run: |
    echo "terraform-version: ${{ steps.stats.outputs.terraform-version }}"
    echo "drift-count: ${{ steps.stats.outputs.drift-count }}"
    echo "resource-drifts: ${{ steps.stats.outputs.resource-drifts }}"
    echo "change-count: ${{ steps.stats.outputs.change-count }}"
    echo "change-percent: ${{ steps.stats.outputs.change-percent }}"
    echo "resource-changes: ${{ steps.stats.outputs.resource-changes }}"
```

## Workflow summary

### :construction: Terraform Stats :construction:

* change-count: 2
* change-percent: 100
* resource-changes:
```json
[
  {
    "address": "docker_container.nginx",
    "changes": [
      "create"
    ]
  },
  {
    "address": "docker_image.nginx",
    "changes": [
      "create"
    ]
  }
]
```

## Inputs

```yml
inputs:
  terraform-directory:
    description: Terraform commands will run in this location.
    required: true
    default: "./terraform"
  include-no-op:
    description: "\"no-op\" refers to the before and after Terraform changes are identical as a value will only be known after apply."
    required: true
    default: false
  add-args:
    description: Pass additional arguments to Terraform plan.
    required: true
    default: ""
  upload-plan:
    description: Upload plan file. true or false
    required: true
    default: false
  upload-retention-days:
    description: Number of days to keep uploaded plan.
    required: true
    default: 7
  plan-file:
    description: Name of plan file.
    required: true
    default: tf__stats__plan.bin
  terraform-version:
    description: Specify a specific version of Terraform
    required: true
    default: latest
```

## Outputs
```yml
outputs:
  terraform-version:
    description: 'Terraform version'
  drift-count:
    description: 'Count of drifts'
  resource-drifts:
    description: 'JSON output of resource drifts'
  change-count:
    description: 'Count of changes'
  change-percent:
    description: 'Percentage of changes to total resources'
  resource-changes:
    description: 'JSON output of resource changes'
```
