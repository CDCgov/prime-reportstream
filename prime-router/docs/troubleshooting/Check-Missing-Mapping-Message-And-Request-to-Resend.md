## **Checking the Missing Mappings Messages and request sender to resend**

Currently, to identify reports with missing observation mappings, we run the following query:

```sql
SELECT action_log.created_at,
       detail ->> 'message'      as message,
       detail ->> 'fieldMapping' as field,
       action_log.report_id,
       report_file.body_url
FROM action_log
         INNER JOIN report_file ON report_file.report_id = action_log.report_id
WHERE action_log.detail ->> 'errorCode' = 'INVALID_MSG_CONDITION_MAPPING' and action_log.detail ->> 'message' LIKE 'Missing mapping for code(s):%'
ORDER BY action_log.created_at DESC
LIMIT 100;
```

### **Handling Missing Mappings**

1. **Identifying Missing Mappings:**
    
    - An example response from the query:
        
        ```json
        {
          "class": "gov.cdc.prime.router.UnmappableConditionMessage",
          "scope": "item",
          "message": "Missing mapping for code(s): 35659-2",
          "errorCode": "INVALID_MSG_CONDITION_MAPPING",
          "fieldMapping": "observation.code.coding.code"
        }
        ```
        
    - If a LOINC code (e.g., `35659-2`) is missing, check if it is mapped in the **Observation Mapping table**.
2. **Checking for Updates:**
    
    - If the LOINC code is not found in the Observation Mapping table:
        - Verify if the **RCTC condition table** has been updated recently.
        - If the code exists in the RCTC table, update our local copy of the Observation Mapping table accordingly.
3. **Handling Unmapped Codes:**
    
    - If the LOINC code is missing from both the Observation Mapping table and the RCTC table:
        - Contact the sender to confirm if it is a **LOCAL LOINC code**.
        - If it is a local code, request that the sender complete the **onboarding sheet** for mapping their conditions.
4. **Reprocessing Messages:**
    
    - Once the missing LOINC code is added to the Observation Mapping table, determine if any messages were **dropped due to the missing mapping**.
    - Document a **clear process** for reprocessing those messages, as this is currently undefined.
    

Existing documentation can be found here:
https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/standard-operating-procedures/update-observation-mapping-table.md

### NOTE: For mor detail of reprocessing the filtered message due to the missing mappings error see [Reprocess Missing Mappings Messages](./Reprocess-Missing-Mappings-Messages.md)