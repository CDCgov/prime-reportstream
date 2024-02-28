# Contributing

## Configuring Git

- Signing commits
- pre-commit hooks

## Coding standards

- sonar
- Kdoc policy

## Code style

Code style is only enforced by pre-commit hooks for kotlin files, and even those checks are not strict about all whitespace/code-style options. Contributors are therefore expected to use consistent code-style/formatting settings to maintain consistency in the repository, and prevent PRs from being muddied by formatting changes.

To apply project settings, in IntelliJ, go to `Settings...`->`Editor`->`Code Style`. Make sure `Project` is selected next to `Scheme:`. Then, go to `Settings...`->`Editor`->`File Types`->`JSON` and under `File name patterns` add `*.fhir`. Optionally, go to `Settings...`->`Tools`->`Actions on Save` and select `Reformat code` to ensure that code is automatically formatted when edited.

Even if you tend to use a different IDE for development, anytime you edit `.kt`, `.yml`, or `.fhir` files, you are expected to use IntelliJ to format those files before opening a PR or merging to the `master` branch.

