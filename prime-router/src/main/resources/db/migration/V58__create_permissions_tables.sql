/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration
*/

/*
    Permissions are type of features that can be associated with an organization.
*/
CREATE TABLE permission (
    -- Key
    permission_id SERIAL PRIMARY KEY,

    -- Value
    name varchar(63) NOT NULL,
    description varchar(2048),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- Metadata
    created_by VARCHAR(63) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

/*
permission_organization links the permission the organization has access to.
It has a many(permissions) to one(organization) relationship.
*/
CREATE TABLE permission_organization (
    -- Key
     permission_organization_id SERIAL PRIMARY KEY,
     permission_id SERIAL REFERENCES permission(permission_id) ON DELETE CASCADE,

    -- Value
     organization_name VARCHAR(63)
);
CREATE INDEX organization_name_idx on permission_organization(organization_name);