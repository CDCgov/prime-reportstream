# YAML CLI Task

## Approach

We will create a CLI task that will validate all our YAML files at one time. This will be crucial as a part of
the CI pipeline.

## Technical details

- Include a flag to determine if we are checking all fhir mappings and fhir transforms in the repo or an external file
  - If an external file, include the path and the type (organization, fhir mapping, or fhir transform)
- Run the json schema code against the files
- Run Konform validation code against the files
- Accumulate all errors if any
  - Do not fail fast to obtain a complete report of problems
- Print out report and return an error so the CI task fails (if running on github)

## Uses

- Run locally to make sure a PR with many YAML changes is ready to be committed
- Github action to ensure all our YAML is valid or else the build fails
