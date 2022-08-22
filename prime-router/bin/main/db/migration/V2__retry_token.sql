/*
 Add error timestamps
 */
ALTER TABLE task ADD COLUMN errored_at TIMESTAMP WITH TIME ZONE;

/*
Add a task column for retry logic. Null without retry information.
*/
ALTER TABLE task
ADD retry_token JSON DEFAULT NULL;

/*
 New error states for tasks after so many retries
 */
ALTER TABLE task ALTER COLUMN next_action TYPE VARCHAR(255);
DROP TYPE IF EXISTS task_action;
CREATE TYPE task_action AS ENUM ('translate', 'batch', 'send', 'wipe', 'none', 'batch_error', 'send_error', 'wipe_error');
ALTER TABLE task ALTER COLUMN next_action TYPE task_action USING next_action::task_action;

