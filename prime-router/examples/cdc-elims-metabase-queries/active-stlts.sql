select  org.name || '.' || receiver.name as "Receiver Name", receiver.created_at as "Created At"
from setting as receiver
join setting org on org.setting_id = receiver.organization_id
where receiver."type" = 'RECEIVER'
and receiver."values" ->> 'topic' = 'elr-elims'
and receiver.is_active = true
and receiver."values" ?? 'transport'
and receiver.is_deleted = false
[[and UPPER( org.name || '.' || receiver.name) like  UPPER('%' || {{name}} || '%')]];
