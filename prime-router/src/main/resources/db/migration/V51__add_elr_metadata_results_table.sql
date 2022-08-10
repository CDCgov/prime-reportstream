/*
* The Flyway tool applies this migration to create the database.
*
* Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
* use VARCHAR(63) for names in organization and schema
*
* Copy a version of this comment into the next migration
*
*/

-- the new elr result metadata table that will hold the information for all types of tests coming in, not just
-- for a specific disease. this has some additional information that the older table does not have
create table if not exists elr_result_metadata
(
    -- our primary key
    elr_result_metadata_id              bigserial
        primary key,
    -- the ID of the report that was sent to us
    report_id                           uuid
        not null
        references report_file on delete cascade,
    -- the index of this record in the file
    report_index                        integer
        not null,
    -- the message ID
    message_id                          varchar(512),
    -- if there's a previous message ID, track that too
    previous_message_id                 varchar(512),
    -- the name of the sending facility
    sender_id                           varchar(512),
    -- their organization name. there are cases where
    -- the sender id could be something like STRAC
    -- but the organization would be the school district
    -- sending through them
    organization_name                   varchar(512),
    -- the topic for the messages coming in
    topic                               varchar(512),
    -- creation date of the record
    created_at                          timestamp
        default CURRENT_TIMESTAMP,

    sending_application_name            varchar(64),
    sending_application_id              varchar(64),

    patient_state                       varchar(512),
    patient_county                      varchar(512),
    patient_country                     varchar(512),
    patient_postal_code                 varchar(64),
    patient_age                         varchar(64),
    patient_gender                      varchar(64),
    patient_gender_code                 varchar(64),
    patient_ethnicity                   varchar(64),
    patient_ethnicity_code              varchar(64),
    patient_race                        varchar(64),
    patient_race_code                   varchar(64),
    patient_tribal_citizenship          varchar(128),
    patient_tribal_citizenship_code     varchar(64),
    patient_preferred_language          varchar(64),
    patient_nationality                 varchar(64),
    patient_species                     varchar(64),
    patient_species_code                varchar(64),

    site_of_care                        varchar(512),

    reason_for_study                    varchar(512),
    reason_for_study_code               varchar(64),

    specimen_collection_date_time       timestamptz,
    specimen_received_date_time         timestamptz,
    specimen_collection_method          varchar(512),
    specimen_collection_site            varchar(512),
    specimen_type                       varchar(512),
    specimen_source_site                varchar(512),

    test_kit_name_id                    varchar(512),
    equipment_model                     varchar(512),

    test_ordered                        varchar(512),
    test_ordered_code                   varchar(512),

    test_performed                      varchar(512),
    test_performed_code                 varchar(512),

    test_result                         varchar(512),
    test_result_code                    varchar(512),

    ordering_provider_id                varchar(512),
    ordering_provider_name              varchar(512),
    ordering_provider_city              varchar(512),
    ordering_provider_state             varchar(512),
    ordering_provider_county            varchar(512),
    ordering_provider_postal_code       varchar(512),

    ordering_facility_id                varchar(512),
    ordering_facility_name              varchar(512),
    ordering_facility_city              varchar(512),
    ordering_facility_county            varchar(512),
    ordering_facility_state             varchar(512),
    ordering_facility_postal_code       varchar(512),

    testing_facility_id                 varchar(512),
    testing_facility_name               varchar(512),
    testing_facility_city               varchar(512),
    testing_facility_state              varchar(512),
    testing_facility_county             varchar(512),
    testing_facility_postal_code        varchar(512)
);

-- comments
comment on table elr_result_metadata
    is 'The elr_result_metadata table is the generic replacement for the COVID result metadata table and will contain results for all ELR messages we receive'
;
comment on column elr_result_metadata.topic
    is 'The topic for the file sent in, such as COVID, or monkeypox, etc'
;
comment on column elr_result_metadata.test_ordered
    is 'The text value for the test ordered'
;
comment on column elr_result_metadata.test_ordered_code
    is 'The LOINC or SNOMED code for the test ordered'
;
comment on column elr_result_metadata.test_performed
    is 'The text value for the test performed'
;
comment on column elr_result_metadata.test_performed_code
    is 'The LOINC or SNOMED code for thet test performed'
;

-- some indexes
create index if not exists elr_result_metadata_idx on
    elr_result_metadata(report_id, report_index)
;

create index if not exists elr_result_message_id_idx on
    elr_result_metadata(message_id)
;

create index if not exists elr_result_created_at_idx on
    elr_result_metadata(created_at)
;

create index if not exists elr_result_metadata_sender_id_idx on
    elr_result_metadata(sender_id)
;

create index if not exists elr_result_metadata_topic_idx on
    elr_result_metadata(topic)
;

create index if not exists elr_result_metadata_state_idx on
    elr_result_metadata(patient_state, ordering_facility_state)
;

create index if not exists elr_result_metadata_org_name_idx on
    elr_result_metadata(organization_name)
;

create index if not exists elr_result_metadata_test_code_idx on
    elr_result_metadata(test_ordered_code, test_performed_code)
;