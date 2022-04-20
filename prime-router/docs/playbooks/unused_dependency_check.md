# Unuused dependency check

We have a github action that is doing a scan of our frontend dependencies, and giving us an idea if any of them are unused. Right now the action will run on the 1st and 15th of each month, and generate a ticket with its findings. 

Click [here](/.github/workflows/dependency_check.yaml) to see the github action workflow file responsible for this check and the ticket creation.

### Ignore list
There are some false positives that you may encounter, and we are utilizing an ignore file at `/frontend-react/.depcheckrc`. You can find more information about depcheck, including the ignore file at the following link. https://www.npmjs.com/package/depcheck

### Template
We are using the following template located [here](/.github/ISSUE_TEMPLATE/dependency_template.md) to create our tickets based on the results of depcheck.