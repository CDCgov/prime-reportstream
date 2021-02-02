# Documentation of the Current Available Logs in Azure

| Log Type           | Type                                    | PII/PHI                                     | Location                     | In Splunk | Required in Splunk for ATO | Notes                                                   |
| :----------------- | :-------------------------------------- | :------------------------------------------ | :--------------------------- | :-------- | :------------------------- | :------------------------------------------------------ |
| Azure Activity Log | Access and environment changes in Azure | No                                          | Subscription > Activity Logs | Yes       | Yes                        |                                                         |
| pdh*-functionapp   | Application logs                        | TBD                                         | In Application Insights      | No        | ?                          |                                                         |
| pdh*-pgsql         | Database errors, access errors, etc     | TBD                                         | Resource > Logs              | No        | ?                          | Could be other types of logs to capture like slow query |
| pdh*-sftpserver    | ?                                       | TBD                                         | Resource > Container > Logs  | No        | ?                          | Is this even in use?                                    |
| Front Door         | Network requests                        | TBD (Do we have PII/PHI as request params?) | Disabled                     | No        | ?                          | We should enable these logs                             |
| WAF                | Firewall denials                        | No                                          | No WAF enabled, so no logs   | No        | ?                          |                                                         |
| primedatahub*      | Access errors, etc                      | No                                          | Resource > Logs              | No        | ?                          |                                                         |


## Potential PII/PHI in Application Logs

1. Batch function logs entire message when received.
2. Send function logs retry and failed messages.
3. Transports can log failures of files. Any concerns of PII in them?

## Potential PII/PHI in Request Logs

1. Download function has a request parameter `file`. Could the file URL have PII in it?
2. Action history checks on some parameters `key` and `code`. Neither look like PII, but would like to verify.