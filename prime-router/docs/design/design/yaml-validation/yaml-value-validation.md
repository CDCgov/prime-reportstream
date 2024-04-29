# YAML Value Validation

## Approach

To validate the configuration values of our YAML files, we will use the Konform library. We will define data classes
corresponding to our yaml files and be able to define constraints on values in an easy-to-read way.

## Library

- The library we are using is konform-kt/konform
    - https://github.com/konform-kt/konform
    - Written in Kotlin to take advantage of native features for expressive DSL
    - Functional approach to validation
    - Reports list of all validation errors and the path to each value
    - Has no underlying dependencies

## Examples

See test code in [ValueValidationTest.kt](./../../../../src/test/kotlin/validation/ValueValidationTest.kt)

