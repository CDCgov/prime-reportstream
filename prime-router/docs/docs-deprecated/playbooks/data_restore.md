# Conditions Requiring Blob Restore

## Storage Failover

If the storage account becomes unavailable in the East US region, you can initiate a failover to promote the secondary
storage location for use within the production system.

### Actions to Mitigate

More in [these Azure docs](https://docs.microsoft.com/en-us/azure/storage/common/storage-disaster-recovery-guidance).

1. In the Azure Portal navigate to the storage account and click the `Geo-replication` blade.
2. Click the `Prepare for failover` button to initiate a failover to the secondary storage location.


## Storage Restore

If an entire storage account gets deleted it can be recovered within 90 days as long as an identically-named storage
account has not been created.

### Actions to Mitigate

1. To restore a storage account, contact the CDC AD helpdesk.
2. At a minimum, provide the subscription ID and the name of the storage account that needs to be restored.  If
   possible, a resource ID would provide the with the most information.


## File Restore

If a file is inadvertently deleted or edited, a previous version of the file can be made the current version within the
60-day retention window.

### Actions to Mitigate

1. In Azure Portal navigate to the appropriate blob container under the `Containers` blade.
2. If a file was deleted, toggle on the `Show deleted blobs` button to be able to see it.
3. Click the deleted or edited file to be restored.
4. Click the `Versions` tab.
5. In the options for the correct version of the file, click `Make current version` to restore the file to the
   appropriate point in time.
