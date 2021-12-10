Organization files have settings for organizations, senders and receivers. These are done as 
incremental settings that are stored in the `prod` and `staging` folders.  For local environment
settings see `src/test/resources/settings/organizations.yml`

The incremental settings serve two purposes. First, they allow us to review and comment on setting changes. Second, they keep a record of the setting changes to rebuild settings in the event that the settings database are lost. A warning these files are not the source of  truth. The settings database is on Azure deployments. 