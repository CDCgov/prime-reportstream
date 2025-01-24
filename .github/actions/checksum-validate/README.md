# Checksum Validate Action

[![Test Action](https://github.com/JosiahSiegel/checksum-validate-action/actions/workflows/test_action.yml/badge.svg)](https://github.com/JosiahSiegel/checksum-validate-action/actions/workflows/test_action.yml)

## Synopsis

1. Generate a checksum from either a string or shell command (use command substitution: `$()`).
2. Validate if checksum is identical to input (even across multiple jobs), using a `key` to link the validation attempt with the correct generated checksum.
   * Validation is possible across jobs since the checksum is uploaded as a workflow artifact

## Usage

```yml
jobs:
  generate-checksums:
    name: Generate checksum
    runs-on: ubuntu-24.04
    steps:
      - uses: actions/checkout@v4.1.1

      - name: Generate checksum of string
        uses: ./.github/actions/checksum-validate@ebdf8c12c00912d18de93c483b935d51582f9236
        with:
          key: test string
          input: hello world

      - name: Generate checksum of command output
        uses: ./.github/actions/checksum-validate@ebdf8c12c00912d18de93c483b935d51582f9236
        with:
          key: test command
          input: $(cat action.yml)

  validate-checksums:
    name: Validate checksum
    needs:
      - generate-checksums
    runs-on: ubuntu-24.04
    steps:
      - uses: actions/checkout@v4.1.1

      - name: Validate checksum of valid string
        id: valid-string
        uses: ./.github/actions/checksum-validate@ebdf8c12c00912d18de93c483b935d51582f9236
        with:
          key: test string
          validate: true
          fail-invalid: true
          input: hello world

      - name: Validate checksum of valid command output
        id: valid-command
        uses: ./.github/actions/checksum-validate@ebdf8c12c00912d18de93c483b935d51582f9236
        with:
          key: test command
          validate: true
          fail-invalid: true
          input: $(cat action.yml)

      - name: Get outputs
        run: |
          echo ${{ steps.valid-string.outputs.valid }}
          echo ${{ steps.valid-command.outputs.valid }}
```

## Workflow summary

### ✅ test string checksum valid ✅

### ❌ test string checksum INVALID ❌

## Inputs

```yml
inputs:
  validate:
    description: Check if checksums match
    default: false
  key:
    description: String to keep unique checksums separate
    required: true
  fail-invalid:
    description: Fail step if invalid checksum
    default: false
  input:
    description: String or command for checksum
    required: true
```

## Outputs
```yml
outputs:
  valid:
    description: True if checksums match
```
