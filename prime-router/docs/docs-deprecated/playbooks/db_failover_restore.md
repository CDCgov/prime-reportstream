# Database Failure Conditions

In the event of an extended PostgreSQL server outage there are two options to restore service. The quickest and most
complete is to promote the read-replica server to a read/write replica. You can also provision a new server from a
point-in-time backup.

In either case you'll need to re-configure any necessary resources (i.e., the Function App and Metabase, at a minimum)
to connect to it. You should do this through Terraform to ensure the configuration is sticky, but you may want to first
test by editing the value in the Azure Portal.


## Actions to Mitigate

More in
[these Azure docs](https://docs.microsoft.com/en-us/azure/postgresql/concepts-read-replicas?msclkid=186f9575ac6d11eca07f9c72ead6d20d#failover-to-replica).


### Promote Read Replica to Read/Write

NOTE: This action is not reversible, so be certain that you want to do this.

1. In the Azure Portal, navigate to the PostgreSQL server that has read/write capabilities.
2. In the `Replication` blade, click the `Stop Replication` button. This will promote the read-replica to a standalone
   read/write instance.
3. Update the connection strings for any resources requiring database access.
3. NOTE: Performance may be degraded if your compute resources are in a different region than your database, so you may
   need to adjust any alert settings.


### Provision New Server From Backup

1. In the Azure Portal, navigate to the PostgreSQL server that has read/write capabilities.
2. In the `Overview` blade, click the `Restore` button.
3. Select the date and time to which to restore a new uniquely named server to.
4. Click `Ok` to provision the new server from the point-in-time backup.