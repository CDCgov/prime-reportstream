#!/bin/bash

tf_dir=$1

#For ["no-op"], the before and
#after values are identical. The "after" value will be incomplete if there
#are values within it that won't be known until after apply.
include_no_op=$2
add_args=$3
plan_file=$4

terraform -chdir=$tf_dir plan $3 -input=false -no-color -lock-timeout=120s -out=$plan_file &>/dev/null
if [[ $? -ne 0 ]]; then
  terraform -chdir=$tf_dir init >/dev/null
  if [[ $? -ne 0 ]]; then
    exit 1
  fi
  terraform -chdir=$tf_dir plan $3 -input=false -no-color -lock-timeout=120s -out=$plan_file >/dev/null
  if [[ $? -ne 0 ]]; then
    exit 1
  fi
fi

PLAN_TXT=$( terraform -chdir=$tf_dir show -no-color $plan_file )
PLAN_JSON=$( terraform -chdir=$tf_dir show -no-color -json $plan_file )

VERSION=$(echo $PLAN_JSON | jq .terraform_version)

# "drift" is changes made outside of terraform
# "resource_drift":
DRIFT=$(echo $PLAN_JSON | jq .resource_drift)
DRIFT_COUNT=$(echo $DRIFT | jq length)
if [[ $DRIFT_COUNT -gt 0 ]]; then
  DRIFTED_RESOURCES=$(echo $DRIFT | jq -c '[.[] | {address: .address, changes: .change.actions}]')
else
  DRIFTED_RESOURCES="[]"
fi

# "changes" is a description of the individual change actions that Terraform
# plans to use to move from the prior state to a new state matching the
# configuration.
# "resource_changes":
CHANGES=$(echo $PLAN_JSON | jq .resource_changes)
if [[ $include_no_op = true ]]; then
  CHANGES_FILTERED=$(echo $CHANGES | jq -c '[.[] | {address: .address, changes: .change.actions}]')
else
  CHANGES_FILTERED=$(echo $CHANGES | jq -c '[.[] | {address: .address, changes: .change.actions} | select( .changes[] != "no-op")]')
fi
CHANGE_COUNT=$(echo $CHANGES_FILTERED | jq length)

# total resources and percent changed
TOTAL_RESOURCES=$(echo $PLAN_JSON | jq -c .planned_values.root_module)
TOTAL_ROOT=$(echo $TOTAL_RESOURCES | jq -c .resources | jq length)
TOTAL_CHILD=$(echo $TOTAL_RESOURCES | jq -c .child_modules | jq -c '[.[]?.resources | length] | add')
TOTAL_COUNT=$(( TOTAL_ROOT + TOTAL_CHILD ))
CHANGE_PERC=$(echo "scale=0 ; $CHANGE_COUNT / $TOTAL_COUNT * 100" | bc)

echo "::set-output name=terraform-version::$(echo $VERSION)"
echo "::set-output name=change-percent::$(echo $CHANGE_PERC)"
echo "::set-output name=drift-count::$(echo $DRIFT_COUNT)"
echo "::set-output name=change-count::$(echo $CHANGE_COUNT)"

# Make output friendly
DRIFTED_RESOURCES="${DRIFTED_RESOURCES//'%'/'%25'}"
DRIFTED_RESOURCES="${DRIFTED_RESOURCES//$'\n'/'%0A'}"
DRIFTED_RESOURCES="${DRIFTED_RESOURCES//$'\r'/'%0D'}"
DRIFTED_RESOURCES="${DRIFTED_RESOURCES//'"'/'\"'}"
echo "::set-output name=resource-drifts::$(echo $DRIFTED_RESOURCES)"
CHANGES_FILTERED="${CHANGES_FILTERED//'%'/'%25'}"
CHANGES_FILTERED="${CHANGES_FILTERED//$'\n'/'%0A'}"
CHANGES_FILTERED="${CHANGES_FILTERED//$'\r'/'%0D'}"
CHANGES_FILTERED="${CHANGES_FILTERED//'"'/'\"'}"
echo "::set-output name=resource-changes::$(echo $CHANGES_FILTERED)"