## Conditions

In the event of a PostgreSQL server outage, there are two options to restore service.  The most expedient, is to promote the read replica server to a read/write replica and configure any necessary resources to connect to it.  Alternatively, a new server can be provisioned and restored from a point-in-time backup and any necessary resources can be configured to connect to the new server.

## Actions

### Promote Read Replica to Read/Write
1. Within the Azure Portal, navigate to the PostgreSQL server that has read/write capabilities.
2. [NOTE] This action is not reversible, so please be certain that you want the read replica to be promoted to a standalone read/write instance.  Within the `Replication` blade, click the `Stop Replication` button.  This will promote the read replica to a standalone read/write instance.
3. Update the connection strings for any resource(s) requiring database access to connect to the new read/write server instance.

### Provision New Server From Backup
1. Within the Azure Portal, navigate to the PostgreSQL server that has read/write capabilities.
2. Within the `Overview` blade, click the `Restore` button.
3. Select the date and time to which to restore a new uniquely named server to.
4. Click `Ok` to provision the new server from the point-in-time backup.