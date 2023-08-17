# Azure

- Azure Function Core Tools (Already exists)
- Azure Function CLI (Already exists)
- Platform Restore (already exists)
- Dashboard (already exists)
- Logs (already exists)
- Database Login (already exists)
- Environment Provisioning (already exists)
- Terraform (already exists)

- ## Data Restore
This section shall cover strategies for restoring data and various Azure services when the need arises.

### Conditions Requiring Blob Restore

#### Storage Failover

If the storage account becomes unavailable in the East US region, you can initiate a failover to promote the secondary
storage location for use within the production system.

More in [these Azure docs](https://docs.microsoft.com/en-us/azure/storage/common/storage-disaster-recovery-guidance).

1. In the Azure Portal navigate to the storage account and click the `Geo-replication` blade.
2. Click the `Prepare for failover` button to initiate a failover to the secondary storage location.


### Storage Restore

If an entire storage account gets deleted it can be recovered within 90 days as long as an identically-named storage
account has not been created.

#### Actions to Mitigate

1. To restore a storage account, contact the CDC AD helpdesk.
2. At a minimum, provide the subscription ID and the name of the storage account that needs to be restored.  If
   possible, a resource ID would provide the user with the most information.


### File Restore

If a file is inadvertently deleted or edited, a previous version of the file can be made the current version within the
60-day retention window.

More in [these Azure docs](https://learn.microsoft.com/en-us/azure/backup/backup-azure-restore-files-from-vm)

1. In Azure Portal navigate to the appropriate blob container under the `Containers` blade.
2. If a file was deleted, toggle on the `Show deleted blobs` button to be able to see it.
3. Click the deleted or edited file to be restored.
4. Click the `Versions` tab.
5. In the options for the correct version of the file, click `Make current version` to restore the file to the
   appropriate point in time.

## DB Failover restore
### Database Failure Conditions

In the event of an extended PostgreSQL server outage there are two options to restore service. The quickest and most
complete is to promote the read-replica server to a read/write replica. You can also provision a new server from a
point-in-time backup.

In either case you'll need to re-configure any necessary resources (i.e., the Function App and Metabase, at a minimum)
to connect to it. You should do this through Terraform to ensure the configuration is sticky, but you may want to first
test by editing the value in the Azure Portal.

More in
[these Azure docs](https://docs.microsoft.com/en-us/azure/postgresql/concepts-read-replicas?msclkid=186f9575ac6d11eca07f9c72ead6d20d#failover-to-replica).


#### Promote Read Replica to Read/Write

NOTE: This action is not reversible, so be certain that you want to do this.

1. In the Azure Portal, navigate to the PostgreSQL server that has read/write capabilities.
2. In the `Replication` blade, click the `Stop Replication` button. This will promote the read-replica to a standalone
   read/write instance.
3. Update the connection strings for any resources requiring database access.
4. NOTE: Performance may be degraded if your compute resources are in a different region than your database, so you may
   need to adjust any alert settings.


#### Provision New Server From Backup

1. In the Azure Portal, navigate to the PostgreSQL server that has read/write capabilities.
2. In the `Overview` blade, click the `Restore` button.
3. Select the date and time to which to restore a new uniquely named server to.
4. Click `Ok` to provision the new server from the point-in-time backup.
