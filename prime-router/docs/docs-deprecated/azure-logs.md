# Current Available Logs in Azure

This is a living document of all logs available to us.

| Priority | In Splunk | Log Type                                                               | Type                                    | PII/PHI | Location                     | Required in Splunk for ATO |
| :------- | :-------- | :--------------------------------------------------------------------- | :-------------------------------------- | :------ | :--------------------------- | :------------------------- |
| 1        | Yes       | Azure Activity Log                                                     | Access and environment changes in Azure | No      | Subscription > Activity Logs | Yes                        |
| 2        | No        | Front Door:<br>- prime-data-hub-test<br>- prime-data-hub-prod          | Network requests                        | No      | Disabled                     | ?                          |
| 3        | No        | WAF (Part of Front Door)                                               | Firewall denials                        | No      | No WAF enabled, so no logs   | ?                          |
| 4        | No        | Storage Account:<br>- pdhteststorageaccount<br>- pdhprodstorageaccount | Access errors, etc                      | No      | Resource > Logs              | ?                          |
| 5        | No        | Database:<br>- pdhtest-pgsql<br>- pdhprod-pgsql                        | Database errors, access errors, etc     | No      | Resource > Logs              | ?                          |
| 6        | No        | Function App:<br>- pdhtest-functionapp<br>- pdhprod-functionapp        | Application logs                        | No      | In Application Insights      | ?                          |
| 7        | No        | Okta                                                                   | Access                                  | No      | In Okta                      | No                         |
| 8        | No        | Akamai (Future)                                                        | Access                                  | No      | In Akamai                    | No                         |
| 9        | No        | SendGrid (Future)                                                      | Email activity                          | TBD     | In SendGrid                  | No                         |
| 10       | No        | Metabase (Future)                                                      | App Service logs                        | No      | Resource > Logs              | No                         |