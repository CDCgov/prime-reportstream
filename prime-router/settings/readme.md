Organization files have settings for organizations, senders and receivers. There are two types of organization files:
1. Complete sets that are used for your local environments
2. Incremental settings that are stored in the `prod` and `staging` folders

The incremental settings serve two purposes. First, they allow us to review and comment on setting changes. Second, they keep a record of the setting changes to rebuild settings in the event that the settings database are lost. A warning these files are not the source of  truth. The settings database is on Azure deployments. 