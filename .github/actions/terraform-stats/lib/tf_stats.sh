#!/bin/bash

tf_dir=$1
#For ["no-op"], the before and
#after values are identical. The "after" value will be incomplete if there
#are values within it that won't be known until after apply.
include_no_op=$2
add_args=$3
plan_file=$4

# Define a function to run terraform plan with common arguments
tf_plan() {
  terraform -chdir=$tf_dir plan $add_args -input=false -no-color -lock-timeout=120s -out=$plan_file "$@"
}

# Try to run terraform plan and init if needed
if ! tf_plan &>/dev/null; then
  terraform -chdir=$tf_dir init >/dev/null || exit 1
  tf_plan >/dev/null || exit 1
fi

# Get the plan output in text and json formats
PLAN_TXT=$( terraform -chdir=$tf_dir show -no-color $plan_file )
PLAN_JSON=$( terraform -chdir=$tf_dir show -no-color -json $plan_file )

# Define a function to parse the plan json with jq
parse_plan_json() {
  echo $PLAN_JSON | jq "$@"
}

# Define a function to make output friendly
make_output_friendly() {
  local output=$1
  output="${output//'%'/'%25'}"
  output="${output//$'\n'/'%0A'}"
  output="${output//$'\r'/'%0D'}"
  output="${output//'"'/'\"'}"
  output="${output//'\\"'/'\\\"'}"
  echo $output
}

# Define a function to write the output to the github output file
write_output() {
  local key=$1
  local value=$2
  echo "$key=$(make_output_friendly $value)" >> $GITHUB_OUTPUT
}

# Get the terraform version from the plan json
VERSION=$(parse_plan_json .terraform_version)

# Get the resource drift from the plan json
DRIFT=$(parse_plan_json .resource_drift)
DRIFT_COUNT=$(echo $DRIFT | jq length)
DRIFTED_RESOURCES=$(echo $DRIFT | jq -c '[.[] | {address: .address, changes: .change.actions}]')

# Get the resource changes from the plan json
CHANGES=$(parse_plan_json .resource_changes)
if [[ $include_no_op = true ]]; then
  CHANGES_FILTERED=$CHANGES
else
  CHANGES_FILTERED=$(echo $CHANGES | jq -c '[.[] | {address: .address, changes: .change.actions} | select( .changes[] != "no-op")]')
fi
CHANGE_COUNT=$(echo $CHANGES_FILTERED | jq length)

# Get the total resources and percent changed from the plan json
TOTAL_RESOURCES=$(parse_plan_json .planned_values.root_module)
TOTAL_ROOT=$(echo $TOTAL_RESOURCES | jq -c .resources | jq length)
TOTAL_CHILD=$(echo $TOTAL_RESOURCES | jq -c .child_modules | jq -c '[.[]?.resources | length] | add')
TOTAL_COUNT=$(( TOTAL_ROOT + TOTAL_CHILD ))
CHANGE_PERC=$(echo "scale=0 ; $CHANGE_COUNT / $TOTAL_COUNT * 100" | bc)

# Write the output to the github output file
write_output "terraform-version" "$VERSION"
write_output "change-percent" "$CHANGE_PERC"
write_output "drift-count" "$DRIFT_COUNT"
write_output "change-count" "$CHANGE_COUNT"
write_output "resource-drifts" "$DRIFTED_RESOURCES"
write_output "resource-changes" "$CHANGES_FILTERED"
