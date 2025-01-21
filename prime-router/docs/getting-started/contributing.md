# Contributing

## Configuring Git

### Committing to this repository

* Commits _must_ be signed or will not be mergeable into `main` or `production` without Repository Administrator intervention. You can find detailed instructions on how to set this up in the [Signing Commits](../getting-started/signing-commits.md) document.
* You will also need to connect to GitHub with an SSH key. You can find instructions on generating and adding an SSH key [here](https://docs.github.com/en/authentication/connecting-to-github-with-ssh).
* Make your changes in topic/feature branches and file a [new Pull Request](https://github.com/CDCgov/prime-reportstream/pulls) to merge your changes in to your desired target branch.

## Git Hooks

We make use of git hooks in this repository and rely on them for certain levels of protections against CI/CD failures and other incidents. Install/activate these hooks by invoking either `prime-router/cleanslate.sh` or by directly invoking `.environment/githooks.sh install`. This is a _repository-level_ setting, you _must_ activate the git hooks in every clone on every device you have.

### pre-commit: Docker

The first hook we'll invoke is to ensure Docker is running. If it's not we'll short-circuit the remainder of the hooks and let you know why.

### pre-commit: Gitleaks

Gitleaks is one of the checks that are run as part of the `pre-commit` hook. It must pass successfully for the commit to proceed (i.e. for the commit to actually happen, failure will prevent the commit from being made and will leave your staged files in staged status). Gitleaks scans files that are marked as "staged" (i.e. `git add`) for known patterns of secrets or keys.

The output of this tool consists of 2 files, both in the root of your repository, which can be inspected for more information about the check:
* `gitleaks.report.json`: the details about any leaks it finds, serialized as JSON. If no leaks are found, this file contains the literal "`null`"; if leaks are found, then this file will contain an array of found leak candidates.
* `gitleaks.log`: the simplified logging output of the gitleaks tool

When gitleaks reports leaks/violations, the right course of action is typically to remove the leak and replace it with a value that is collected at run-time. There are limited cases where the leak is a false positive, in which case a _strict and narrow_ exemption may be added to the `.environment/gitleaks/gitleaks-config.toml` configuration file. _If an exemption is added, it must be signed off on by a member of the DevOps team_.

This tool can also be manually invoked through `.environment/gitleaks/run-gitleaks.sh` which may be useful to validate the lack of leaks without the need of risking a commit. Invoke the tool with `--help` to find out more about its different run modes.

See [Allow-listing Gitleaks False Positives](../docs-deprecated/allowlist-gitleaks-false-positives.md) for more details on how to prevent False Positives!

### pre-commit: Terraform formatting

If you've changed any terraform files in your commit we'll run
`terraform fmt -check` against the directory of files. If any file's format is invalid 
the pre-commit hook will fail. You may be able to fix the issues with:

```bash
$ terraform fmt -recursive
```

## Coding standards

- sonar
- Kdoc policy

## Code style

### General expectations

Code style is only enforced by pre-commit hooks for kotlin files, and even those checks are not strict about all whitespace/code-style options. Contributors are therefore expected to use consistent code-style/formatting settings to maintain consistency in the repository, and prevent PRs from being muddied by formatting changes.

To apply project settings, in IntelliJ, go to `Settings...`->`Editor`->`Code Style`. Make sure `Project` is selected next to `Scheme:`. Then, go to `Settings...`->`Editor`->`File Types`->`JSON` and under `File name patterns` add `*.fhir`. Optionally, go to `Settings...`->`Tools`->`Actions on Save` and select `Reformat code` to ensure that code is automatically formatted when edited.

Even if you tend to use a different IDE for development, anytime you edit `.kt`, `.yml`, or `.fhir` files, you are expected to use IntelliJ to format those files before opening a PR or merging to the `main` branch.

### Kotlin

The Kotlin code in the project follows the KTLint style guide. There is a git hook that checks for conformance to this style guide. To reformat your new code to be in compliance:

```bash
gradle ktlintFormat
```