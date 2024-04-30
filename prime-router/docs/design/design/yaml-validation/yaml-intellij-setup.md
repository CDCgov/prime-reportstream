# IntelliJ IDEA Setup

Getting Intellij setup to report errors during coding greatly speeds up development and reduces errors.

## Apply Schema

Add the following comment to the top of your yaml file to enable Intellij validation. Ensure the path to the schema 
is correct.
```yaml
# $schema: ./../../../metadata/json_schema/schema.json
```
You can also register the schema with Intellij and apply it to certain files and directories. I don't recommend doing
it this way as tracking it in the files ensure others don't have to continually update their Intellij settings to stay
up to date on schema locations and new files.

## Set up red underline on errors

- Preferences -> Editor -> Inspections
- Scroll down and select YAML
- Select `Validation by JSON Schema`
- Switch `Severity` to `Error`
  - This will ensure you get the red underline for YAML issues
