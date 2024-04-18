# YAML CLI Task

## Approach

We will create a CLI task that will validate all our YAML files at one time. This will be crucial as a part of
the CI pipeline.

## Uses

- Run locally to make sure a PR with many YAML changes is ready to be committed
- Github action to ensure all our YAML is valid or else the build fails
