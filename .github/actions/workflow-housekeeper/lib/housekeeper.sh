#!/bin/bash

# Init
repo="$1"
ignore_branch_workflows=$2
retention_time=$3
retain_run_count=$4
dry_run=$5

# ignore_branch_workflows
if [[ -z $ignore_branch_workflows ]]; then
  ignore_branch_workflows=false
fi
echo "ignore_branch_workflows: $ignore_branch_workflows"

if [[ $ignore_branch_workflows = true ]]; then
  files=$(ls -1 .github/workflows/ | sed 's/^/and .path != \".github\/workflows\//;s/$/\"/')
else
  files=""
fi

# retention_time
if [[ -z $retention_time ]]; then
  retention_time=""
else
  keep_date=$(date -d "$date -$retention_time" +%s)
  keep_stmt="| select (.run_started_at | . == null or fromdateiso8601 < $keep_date)"
fi
echo "time_threshold: $retention_time"

# retain_run_count
if [[ -z $retain_run_count ]]; then
  retain_run_count=0
fi
let retain_run_count2=retain_run_count*2
echo "retain_run_count: $retain_run_count2"

# dry_run
if [[ -z $dry_run ]]; then
  dry_run=false
fi
echo "dry_run: $dry_run"

# Build jq query
runs="repos/$repo/actions/runs"
query=".workflow_runs[] \
| select( \
.path != \".github/workflows/placeholder.yaml\" \
"$files"
)
$keep_stmt
| (.path,.id)"

# Get run ids
output=$(gh api --paginate $runs --jq "$query")
output=($(echo $output | tr " " "\n"))
output=${output[@]:$retain_run_count2}

# Delete or echo run ids
for id in $output; do
  if [[ $dry_run = false ]]; then
    [[ $id != ".git"* ]] && gh api --silent $runs/$id -X DELETE
  else
    [[ $id != ".git"* ]] && echo "gh api --silent $runs/$id -X DELETE" || echo "$id"
  fi
  [[ $id = ".git"* ]] && summary+="  * $id" || summary+=" $id\n"

  # Prevent rate limit
  sleep 1;
done

echo "housekeeping_output=$(echo "${summary}")" >>$GITHUB_OUTPUT
