## Conditions

There is a batch or send operation that has been queued, but we would
like to prevent these operations from taking place. In other words, we want
to disrupt the normal workflow. 

## Prerequisites

1. Connection to production database
2. PSQL or equivalent tool that supports DB queries

## Actions

1. Find report id to act on. 

    ```
    SELECT report_id, next_action, created_at FROM task WHERE retry_token is not null;
    ```
2. Set the `next_action` column to `none`

    ```
    UPDATE task
    SET next_action = 'none'
    WHERE report_id = '673bcfe9-3d4e-4aaa-97eb-a0f8e173d706';
    ```
   