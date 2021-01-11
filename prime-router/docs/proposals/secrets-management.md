## Background

TODO

## Goals

## Proposal

### Tiering Secret Storage

Due to multiple needs and different secrets storage strategies, our secrets storage should be broken up into two different tiers.

#### Tier 1: Application Secrets

These are secrets the core application needs to function: database connection strings, tokens for external core-application services like Okta, etc. These secrets should be stored in environment variables that can easily be configured on local developer machines and cloud services.

#### Tier 2: Client Secrets / Hub Connection Secrets

The second tier of secrets are that scale with the number of connections the Hub makes: SFTP passwords, FIHR tokens, HL7 tokens, etc. These are secrets that are not essential to the application starting up, but they are essential for data to arrive successfully at health departments, etc.

Since these secrets are going to continue to grow as we make more an more connections to the Hub, these secrets should not be stored in environment variables. Instead, the data hub application code itself should fetch any secrets needed to make a connection on demand when needed. We should build a connections secret service that is storage-agnostic and follow an inheritance strategy for the connection class, so multiple different connection types can be stored. This will allow us to separate the concerns and leverage different secrets storage strategies and security requirements evolve.

To enable storage-agnostic secrets access, we should build a base service that can be extended depending on the storage mechanism. Method signatures for this class may look like the following:

```
abstract class ConnectionCredentialStorageService {
    ConnectionCredential fetchConnectionCredential(UUID connectionId)
    void saveConnectionCredential(UUID connectionId, ConnectionCredential credential)
}
```

This assume that there will only ever be one set of credentials for each connection. Under this design, if we had multiple credentials for the same client, we would create multiple connections in our system to store each. This could be extended outside of the secrets management service to handle more complex management around handling of multiple credentials for the same client (i.e. sending data to all connections, or building schedules, etc.).

From there, the `ConnectionCredential` would be an interface that would be extended depending on the connection type. For example, a couple of different `ConnectionCredential` classes may look like:

```
abstract class ConnectionCredential {
    String host;
    Integer port;
}

class SFTPConnectionCredential extends ConnectionCredential {
    String username;
    String password;
}

class HL7ConnectionCredential extends ConnectionCredential {
    String token;
}
```


### Storing Secrets Per Environment

With a `ConnectionCredentialStorageService` in place, we can store secrets slightly differently depending on the environment.

#### Storing Secrets Locally

Locally, we should store secrets in a way that leverages the same access methods as we will in th cloud.

For applications secrets, we should set local environment variables. Any application secrets that need to be shared between developers should be communicated via a secure method when setting up the developer's environment.

For connection secrets, we should leverage Hashicorp Vault. Hashicorp Vault provides a Docker container we can configure to spin up with our existing Docker containers. The container should be configured to use local storage, which persists across Docker environment tear downs. The local storage file will be added to our `.gitignore` to ensure the secrets database remains unique per developer. The local storage database will be encrypted using a Vault secret, so in the event a developer's local secret database is compromised, the attacker would still require the Vault secret.

#### Storing Secrets in Higher Cloud Environments

In the cloud, we should leverage Azure Key Vault for secrets management. Key Vault is highly available, scales, and is affordable for our needs. The underlying Key Vault service is managed by Microsoft, so there is no additional infrastructure management costs on our end. When making this decision a comparison was weighed against standing up a Hashicorp Vault Cluster in the cloud.

|                            | Azure Key Vault                                                                                    | Hashicorp Vault                                                                                                                                                        |
| -------------------------- | :------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Infrastructure**         | None, managed by Azure                                                                             | For high availability tolerance, 2-3 instances per environment at a minimum                                                                                            |
| **Security**               | High, supports complex ACLs, HSM keys, key and secret rotation                                     | High, supports complex ACLs, HSM keys, key and secret rotation                                                                                                         |
| **Ongoing Management**     | None, outside of provisioning                                                                      | Medium, additional administration required to keep cluster up-to-date and scale the cluster as we grow                                                                 |
| **Cost**                   | Medium, our secrets access model will generate lots of transactions, but costs will scale with use | Medium, costs won't directly be tied to transactions, but with growth, we might require a complex cluster that requires extra developer time to administer and upgrade |
| **Risk**                   | Low, main risk is on initial provisioning if misconfigured                                         | Medium, with on-going maintenance, whenever anything changed, there is always a risk of misconfiguration, leading to an outage or security incident                    |
| **Infrastructure Support** | High, support provided by Azure, matching our Azure contract                                       | None, unless we acquire a Vault enterprise license                                                                                                                     |
| **Dev Support**            | High, lots of client provided libraries                                                            | High, lots of client provided libraries                                                                                                                                |

##### Cloud Design

In our cloud environment, we should create a separate vault for each tier of secrets (i.e. a separate vault for application secrets and a separate vault for client secrets). Each environment should have their own set of vaults that is not shared with any other environment.

ACL should be leverage so that there are separate access policies per vault. In high environments like production, developers should not be given access to read secrets from the vault. Long term, for client secrets, the application itself should handle creating an updating connection secrets and that should be be done through the Azure console.