Organization files have settings for organizations, senders and receivers. These are done as 
incremental settings that are stored in the `prod` and `staging` folders.  For local environment
settings see `src/test/resources/settings/organizations.yml`

The incremental settings serve two purposes. First, they allow us to review and comment on setting changes. Second, they keep a record of the setting changes to rebuild settings in the event that the settings database are lost. A warning these files are not the source of  truth. The settings database is on Azure deployments.

## Settings schemas

### json schemas for settings validation:

**settings schemas are in settings/schemas directory:**

- Settings: settings/schemas/settings.json - schema for organizations.yml, an array of organization
- Organization: settings/schemas/organization.json - schema for a single organization including 0 or many sender(s)
- Sender: settings/schemas/sender.json - schema for a single sender, a sender belongs to an organization
- Receiver: settings/schemas/receiver.json - schema for a single receiver, a receiver belongs to an organization

Settings schemas are used at the Settings API to validate the data

Settings schemas can also be used locally to validate data in yaml files, see below examples:

Validate the local organizations.yml

```bash
./prime-no-debug validate-setting -i settings/organizations.yml -s settings/schemas/settings.json
```
Validate a single sender object

```bash
./prime-no-debug validate-setting -i src/test/unit_test_files/one_sender_waters.yml -s settings/schemas/sender.json
```
