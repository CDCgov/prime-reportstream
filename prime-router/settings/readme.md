Organization files have settings for organizations, senders and receivers. These are done as 
incremental settings that are stored in the `prod` and `staging` folders.  For local environment
settings see `src/test/resources/settings/organizations.yml`

The incremental settings serve two purposes. First, they allow us to review and comment on setting changes. Second, they keep a record of the setting changes to rebuild settings in the event that the settings database are lost. A warning these files are not the source of truth. The settings database is on Azure deployments.

## Settings schemas

### json schemas for settings validation:

**settings schemas are in src/main/resources/settings/schemas directory:**

- Settings: settings.json - schema for organizations.yml, an array of organization
- Organization: organization.json - schema for a single organization including 0 or many sender(s) or receiver(s)
- Sender: sender.json - schema for a single sender, a sender belongs to an organization
- Receiver: receiver.json - schema for a single receiver, a receiver belongs to an organization

Settings schemas are used by the Settings API to validate the data

Settings schemas can also be used locally to validate data in yaml files, see below examples:

Validate the local organizations.yml

```bash
./prime validate-setting -i settings/organizations.yml -s src/main/resources/settings/schemas/settings.json
```
Validate a single sender object

```bash
./prime validate-setting -i src/test/unit_test_files/one_sender_waters.yml -s src/main/resources/settings/schemas/sender.json
```

Validate a single receiver object

```bash
./prime validate-setting -i src/test/unit_test_files/one_receiver_waters.yml -s src/main/resources/settings/schemas/receiver.json
```
