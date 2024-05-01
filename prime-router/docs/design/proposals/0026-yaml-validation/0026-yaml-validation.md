# `YAML` Validation Proposal

## Background

Our application uses `YAML` in a variety of contexts for configuration. It would speed up development and reduce runtime 
errors if we were able to validate the `YAML` structure and value types. We want to catch these issues at compile-time during
development.

## Assumptions

* We will want to continue using `YAML` as our primary configuration language
* We will want our `YAML` validated every time we either read or update a configuration

## Possible Implementations

### Use Apple's `Pkl` configuration library ([link](https://pkl-lang.org/index.html))

#### Pros

* Much more readable syntax than either `YAML` or `json`
* Allows the user to define reusable configuration types to use across different configurations
* Will do compile-time checks for configuration to ensure it matches the defined schema
* Can specify validation rules for configuration values for further safety
* Has plugins for a variety of IDEs to further prevent errors with autocomplete and syntax checks
* Automatically generates data classes for easy serialization and use in code

#### Cons

* We would have to rewrite all our configuration to be `pkl` files
  * At the moment, the library does not support running existing `YAML` files through validation
* We would need to rewrite many of our CLI commands that currently accept `YAML` to now accept `Pkl`

#### Decision

This would meet all of our needs if we were creating a new app but with the plethora of existing `YAML` files,
we need a solution that works for the codebase we have, not the one we want.

### Write our own validation code with Jackson Serialization

#### Approach

* Create a data class structure that matches our configuration (starting with organizations.yml)
* Serialize it with a strict Jackson `YAML` mapper that throws errors on missing fields or extra fields
* Run custom validation code on it to ensure configuration values are acceptable

#### Pros

* This would be the most flexible approach for us as we could write validation code very specific to Report Stream that could not be captured otherwise
  * Example: ensure filter definitions are appropriately formatted

#### Cons

* None of the work will be outsourced to existing libraries and would have to all be done in-house
* The validations would be spread across multiple files in various places and could be hard to find
* The configuration data classes would have to change every time we made an update

#### Decision

This idea would work but may require a lot of custom work. This would be a good choice if we want very in-depth validation.

### Write JSON Schema files and run validations against it

#### Approach
* Create JSON Schema files to test our YAML files against to ensure the proper structure
* Use a library to test our deserialized configuration values
  * We will be using the [Konform library](https://github.com/konform-kt/konform)

#### Pros

* Off the shelf standardized solution
* All validations should be contained in one spot for easy locating

#### Cons

* Lose out on extra validations for values beyond types

#### Decision

This is presently the preferred solution as it has the least upfront work with the greatest return. It does not
provide us with the very Report Stream specific validations, but it will catch the type, structure, and typo errors 
which is most of the battle.
