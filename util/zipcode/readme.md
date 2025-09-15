
# **Standard Operating Procedure (SOP): Updating Missing ZIP Codes in the Lookup Table**

## **Purpose**

This document describes the procedure for identifying, retrieving, and updating missing ZIP code entries in the `zip-code-data.csv` file and applying those updates to the ReportStream ZIP code lookup table.

---

## **Prerequisites**

- A valid HUD API token to retrieve ZIP code data

    - Obtain from: [HUD API Registration](https://www.huduser.gov/hudapi/public/register?comingfrom=1)

    - Select: **USPS ZIP CODE CROSSWALK** as the dataset

- Production OKTA access to run the `update-lookup-table` command

- Access to edit and commit changes to the repository

- Python installed


---

## **Background**

Missing ZIP codes in the lookup table can lead to transformation failures and routing issues in ReportStream. This SOP ensures all ZIP codes have complete data including state, FIPS code, county, and city.

---

## **Step-by-Step Instructions**

### **Step 1: Identify Missing ZIP Codes**

1. Open the [UP Message Monitoring Dashboard](https://portal.azure.com/#@cdc.onmicrosoft.com/dashboard/arm/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourcegroups/prime-data-hub-test/providers/microsoft.portal/dashboards/9a35cfea-cebd-4c9e-9a63-32c5d510d528).

2. Review the **[Prod] Zip Code Lookup Failures (30 Days)** tile for recent failures.

3. (Optional) Run the following Kusto query in Azure Monitor to extract ZIPs directly:

    ```kusto
    traces
    | where timestamp > ago(30d)
    and message has "getStateFromZipCode()"
    and isnotempty(operation_Name)
    | extend cd_json = parse_json(customDimensions)
    | project ZipCode = split(message, ":")[-1]
    | distinct tostring(ZipCode)
    ```

4. Export the results to a CSV file for input into the script.

---

### **Step 2: Lookup ZIP Code Details**

#### One-time Setup

```bash
python3 -m venv env
source env/bin/activate
pip install requests
```

#### Run Lookup Script

```bash
python3 zipcode.py <input_file.csv> <HUD_API_token>
```
- This script uses the HUD USPS Crosswalk API to retrieve details (state, city, county name, and FIPS codes) for each missing ZIP code.
- The script will update your local copy of [`zip-code-data.csv`](https://chatgpt.com/prime-router/metadata/tables/local/zip-code-data.csv).

---

### **Step 3: Update the Lookup Table in Staging**

1. Login to the Prime CLI in staging:

    ```bash
    ./prime login --env staging
    ```

2. Update the lookup table with the new data:

    ```bash
    ./prime lookuptables create -n zip-code-data -i metadata/tables/local/zip-code-data.csv -a --env staging
    ```

3. Confirm the changes and verify the output for any issues.

---

### **Step 4: Promote to Production**

Once validated in staging:

1. Log in to production:

    ```bash
    ./prime login --env prod
    ```

2. Repeat the table update in production:

    ```bash
    ./prime lookuptables create -n zip-code-data -i metadata/tables/local/zip-code-data.csv -a --env prod
    ```

3. Verify completion and success.


---

### **Step 5: Commit and Push Changes to Git**

1. Create a pull request to commit the updated lookup table to version control:

    ```bash
    git add zip-code-data.csv
    git commit -m "Add missing ZIP codes to lookup table"
    git push origin <your-branch>
    ```

2. Follow standard team procedures for code review and merge.