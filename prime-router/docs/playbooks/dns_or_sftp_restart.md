## Conditions

In the event the DNS or SFTP containers terminate unexpectingly they can be restarted via the command line.

### Actions

## Login to the Azure CLI

```
az login
```

## Restart the container

```
az container restart --name [container_name] --resource-group [resource_group] --no-wait
```