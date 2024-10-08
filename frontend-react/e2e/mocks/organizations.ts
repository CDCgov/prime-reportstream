export const MOCK_GET_RECEIVERS_IGNORE = [ {
    "name" : "QUALITY_ALL",
    "organizationName" : "ignore",
    "topic" : "covid-19",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "empty",
        "format" : "CSV",
        "useBatching" : false,
        "defaults" : { },
        "nameFormat" : "STANDARD",
        "receivingOrganization" : null,
        "type" : "CUSTOM"
    },
    "jurisdictionalFilter" : [ "matches(ordering_facility_state, IG)", "matches(ordering_facility_county, QUALITY_ALL)" ],
    "qualityFilter" : [ "allowAll()" ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ ],
    "reverseTheQualityFilter" : false,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : null,
    "description" : "",
    "transport" : null,
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : null,
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "QUALITY_FAIL",
    "organizationName" : "ignore",
    "topic" : "covid-19",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "empty",
        "format" : "CSV",
        "useBatching" : false,
        "defaults" : { },
        "nameFormat" : "STANDARD",
        "receivingOrganization" : null,
        "type" : "CUSTOM"
    },
    "jurisdictionalFilter" : [ "matches(ordering_facility_state, IG)", "matches(ordering_facility_county, QUALITY_FAIL)" ],
    "qualityFilter" : [ "hasValidDataFor(blankField)", "hasAtLeastOneOf(message_id,blankField)" ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ ],
    "reverseTheQualityFilter" : false,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : null,
    "description" : "",
    "transport" : null,
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : null,
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "HL7",
    "organizationName" : "ignore",
    "topic" : "covid-19",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "fl/fl-covid-19",
        "format" : "HL7",
        "useBatching" : false,
        "defaults" : { },
        "nameFormat" : "STANDARD",
        "receivingOrganization" : null,
        "type" : "CUSTOM"
    },
    "jurisdictionalFilter" : [ "matches(ordering_facility_state, IG)", "matches(ordering_facility_county, HL7)" ],
    "qualityFilter" : [ ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ ],
    "reverseTheQualityFilter" : false,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : {
        "operation" : "MERGE",
        "numberPerDay" : 1440,
        "initialTime" : "00:00",
        "timeZone" : "EASTERN",
        "maxReportCount" : 100,
        "whenEmpty" : {
            "action" : "NONE",
            "onlyOncePerDay" : false
        }
    },
    "description" : "",
    "transport" : {
        "host" : "172.17.6.20",
        "port" : "22",
        "filePath" : "./upload",
        "credentialName" : null,
        "type" : "SFTP"
    },
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : null,
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "QUALITY_PASS",
    "organizationName" : "ignore",
    "topic" : "covid-19",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "empty",
        "format" : "CSV",
        "useBatching" : false,
        "defaults" : { },
        "nameFormat" : "standard",
        "receivingOrganization" : null,
        "type" : "CUSTOM"
    },
    "jurisdictionalFilter" : [ "matches(ordering_facility_state, IG)", "matches(ordering_facility_county, QUALITY_PASS, removed)" ],
    "qualityFilter" : [ "hasValidDataFor(message_id,ordering_facility_county,ordering_facility_state)", "hasAtLeastOneOf(message_id,blankField)", "matches(ordering_facility_county, QUALITY_PASS)" ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ ],
    "reverseTheQualityFilter" : false,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : null,
    "description" : "",
    "transport" : null,
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : null,
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "QUALITY_REVERSED",
    "organizationName" : "ignore",
    "topic" : "covid-19",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "empty",
        "format" : "CSV",
        "useBatching" : false,
        "defaults" : { },
        "nameFormat" : "standard",
        "receivingOrganization" : null,
        "type" : "CUSTOM"
    },
    "jurisdictionalFilter" : [ "matches(ordering_facility_state, IG)", "matches(ordering_facility_county, QUALITY_REVERSED, kept)" ],
    "qualityFilter" : [ "hasValidDataFor(message_id,ordering_facility_county,ordering_facility_state)", "hasAtLeastOneOf(message_id,blankField)", "matches(ordering_facility_county, QUALITY_REVERSED)" ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ ],
    "reverseTheQualityFilter" : true,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : null,
    "description" : "",
    "transport" : null,
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : null,
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "BLOBSTORE",
    "organizationName" : "ignore",
    "topic" : "covid-19",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "hhsprotect/hhsprotect-covid-19",
        "format" : "CSV",
        "useBatching" : false,
        "defaults" : { },
        "nameFormat" : "STANDARD",
        "receivingOrganization" : null,
        "type" : "CUSTOM"
    },
    "jurisdictionalFilter" : [ "matches(ordering_facility_state, IG)", "matches(ordering_facility_county, BLOBSTORE)" ],
    "qualityFilter" : [ ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ ],
    "reverseTheQualityFilter" : false,
    "deidentify" : true,
    "deidentifiedValue" : "",
    "timing" : {
        "operation" : "MERGE",
        "numberPerDay" : 1440,
        "initialTime" : "00:00",
        "timeZone" : "EASTERN",
        "maxReportCount" : 100,
        "whenEmpty" : {
            "action" : "NONE",
            "onlyOncePerDay" : false
        }
    },
    "description" : "",
    "transport" : {
        "storageName" : "PartnerStorage",
        "containerName" : "hhsprotect",
        "type" : "BLOBSTORE"
    },
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : null,
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "CSV",
    "organizationName" : "ignore",
    "topic" : "covid-19",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "az/pima-az-covid-19",
        "format" : "CSV",
        "useBatching" : false,
        "defaults" : { },
        "nameFormat" : "STANDARD",
        "receivingOrganization" : null,
        "type" : "CUSTOM"
    },
    "jurisdictionalFilter" : [ "matches(ordering_facility_state, IG)", "matches(ordering_facility_county, CSV)" ],
    "qualityFilter" : [ ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ ],
    "reverseTheQualityFilter" : false,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : {
        "operation" : "MERGE",
        "numberPerDay" : 1440,
        "initialTime" : "00:00",
        "timeZone" : "EASTERN",
        "maxReportCount" : 100,
        "whenEmpty" : {
            "action" : "NONE",
            "onlyOncePerDay" : false
        }
    },
    "description" : "For testing only.",
    "transport" : {
        "host" : "172.17.6.20",
        "port" : "22",
        "filePath" : "./upload",
        "credentialName" : null,
        "type" : "SFTP"
    },
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : null,
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "HL7_NULL",
    "organizationName" : "ignore",
    "topic" : "covid-19",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "tx/tx-covid-19",
        "format" : "HL7_BATCH",
        "useBatching" : false,
        "defaults" : { },
        "nameFormat" : "STANDARD",
        "receivingOrganization" : null,
        "type" : "CUSTOM"
    },
    "jurisdictionalFilter" : [ "matches(ordering_facility_state, IG)", "matches(ordering_facility_county, HL7_NULL)" ],
    "qualityFilter" : [ ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ ],
    "reverseTheQualityFilter" : false,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : {
        "operation" : "MERGE",
        "numberPerDay" : 1440,
        "initialTime" : "00:00",
        "timeZone" : "EASTERN",
        "maxReportCount" : 100,
        "whenEmpty" : {
            "action" : "NONE",
            "onlyOncePerDay" : false
        }
    },
    "description" : "",
    "transport" : {
        "dummy" : null,
        "type" : "NULL"
    },
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : null,
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "EVERY_5_MINS",
    "organizationName" : "ignore",
    "topic" : "covid-19",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "covid-19",
        "useTestProcessingMode" : false,
        "useBatchHeaders" : true,
        "receivingApplicationName" : null,
        "receivingApplicationOID" : null,
        "receivingFacilityName" : null,
        "receivingFacilityOID" : null,
        "messageProfileId" : null,
        "replaceValue" : { },
        "replaceValueAwithB" : { },
        "reportingFacilityName" : null,
        "reportingFacilityId" : null,
        "reportingFacilityIdType" : null,
        "suppressQstForAoe" : false,
        "suppressHl7Fields" : null,
        "suppressAoe" : false,
        "defaultAoeToUnknown" : false,
        "replaceUnicodeWithAscii" : false,
        "useBlankInsteadOfUnknown" : null,
        "truncateHDNamespaceIds" : false,
        "truncateHl7Fields" : null,
        "usePid14ForPatientEmail" : false,
        "convertTimestampToDateTime" : null,
        "cliaForOutOfStateTesting" : null,
        "cliaForSender" : { },
        "phoneNumberFormatting" : "STANDARD",
        "suppressNonNPI" : false,
        "processingModeCode" : null,
        "replaceDiiWithOid" : null,
        "applyOTCDefault" : false,
        "useOrderingFacilityName" : "STANDARD",
        "valueSetOverrides" : { },
        "nameFormat" : "standard",
        "receivingOrganization" : null,
        "convertPositiveDateTimeOffsetToNegative" : false,
        "stripInvalidCharsRegex" : null,
        "convertDateTimesToReceiverLocalTime" : false,
        "useHighPrecisionHeaderDateTimeFormat" : false,
        "type" : "HL7",
        "truncationConfig" : {
            "truncateHDNamespaceIds" : false,
            "truncateHl7Fields" : [ ],
            "customLengthHl7Fields" : { }
        }
    },
    "jurisdictionalFilter" : [ "matches(ordering_facility_state, IG)", "matches(ordering_facility_county, EVERY_5_MINS)" ],
    "qualityFilter" : [ ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ ],
    "reverseTheQualityFilter" : false,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : {
        "operation" : "MERGE",
        "numberPerDay" : 288,
        "initialTime" : "00:00",
        "timeZone" : "EASTERN",
        "maxReportCount" : 100,
        "whenEmpty" : {
            "action" : "NONE",
            "onlyOncePerDay" : false
        }
    },
    "description" : "",
    "transport" : {
        "host" : "172.17.6.20",
        "port" : "22",
        "filePath" : "./upload",
        "credentialName" : null,
        "type" : "SFTP"
    },
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : "Batches and Sends every 5 minutes.  For Load testing.",
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "EVERY_15_MINS",
    "organizationName" : "ignore",
    "topic" : "covid-19",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "covid-19",
        "useTestProcessingMode" : false,
        "useBatchHeaders" : true,
        "receivingApplicationName" : null,
        "receivingApplicationOID" : null,
        "receivingFacilityName" : null,
        "receivingFacilityOID" : null,
        "messageProfileId" : null,
        "replaceValue" : { },
        "replaceValueAwithB" : { },
        "reportingFacilityName" : null,
        "reportingFacilityId" : null,
        "reportingFacilityIdType" : null,
        "suppressQstForAoe" : false,
        "suppressHl7Fields" : null,
        "suppressAoe" : false,
        "defaultAoeToUnknown" : false,
        "replaceUnicodeWithAscii" : false,
        "useBlankInsteadOfUnknown" : null,
        "truncateHDNamespaceIds" : false,
        "truncateHl7Fields" : null,
        "usePid14ForPatientEmail" : false,
        "convertTimestampToDateTime" : null,
        "cliaForOutOfStateTesting" : null,
        "cliaForSender" : { },
        "phoneNumberFormatting" : "STANDARD",
        "suppressNonNPI" : false,
        "processingModeCode" : null,
        "replaceDiiWithOid" : null,
        "applyOTCDefault" : false,
        "useOrderingFacilityName" : "STANDARD",
        "valueSetOverrides" : { },
        "nameFormat" : "standard",
        "receivingOrganization" : null,
        "convertPositiveDateTimeOffsetToNegative" : false,
        "stripInvalidCharsRegex" : null,
        "convertDateTimesToReceiverLocalTime" : false,
        "useHighPrecisionHeaderDateTimeFormat" : false,
        "type" : "HL7",
        "truncationConfig" : {
            "truncateHDNamespaceIds" : false,
            "truncateHl7Fields" : [ ],
            "customLengthHl7Fields" : { }
        }
    },
    "jurisdictionalFilter" : [ "matches(ordering_facility_state, IG)", "matches(ordering_facility_county, EVERY_15_MINS)" ],
    "qualityFilter" : [ ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ ],
    "reverseTheQualityFilter" : false,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : {
        "operation" : "MERGE",
        "numberPerDay" : 96,
        "initialTime" : "00:00",
        "timeZone" : "EASTERN",
        "maxReportCount" : 100,
        "whenEmpty" : {
            "action" : "NONE",
            "onlyOncePerDay" : false
        }
    },
    "description" : "",
    "transport" : {
        "host" : "172.17.6.20",
        "port" : "22",
        "filePath" : "./upload",
        "credentialName" : null,
        "type" : "SFTP"
    },
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : "Batches and Sends every 15 minutes.  For Load testing.",
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "SETTINGS_TEST",
    "organizationName" : "ignore",
    "topic" : "covid-19",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "empty",
        "format" : "CSV",
        "useBatching" : false,
        "defaults" : { },
        "nameFormat" : "standard",
        "receivingOrganization" : null,
        "type" : "CUSTOM"
    },
    "jurisdictionalFilter" : [ "matches(ordering_facility_county, SETTINGS_TEST)" ],
    "qualityFilter" : [ "allowAll()" ],
    "routingFilter" : [ "matches(sender_fullname, ignore.ignore-empty)", "matches(sender_orgname, ignore)" ],
    "processingModeFilter" : [ "matches(processing_mode_code, P)" ],
    "reverseTheQualityFilter" : false,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : {
        "operation" : "MERGE",
        "numberPerDay" : 1440,
        "initialTime" : "00:00",
        "timeZone" : "EASTERN",
        "maxReportCount" : 100,
        "whenEmpty" : {
            "action" : "NONE",
            "onlyOncePerDay" : false
        }
    },
    "description" : "",
    "transport" : {
        "host" : "172.17.6.20",
        "port" : "22",
        "filePath" : "./upload",
        "credentialName" : null,
        "type" : "SFTP"
    },
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : "Used to test putting sender settings into the data, using ignore.ignore-empty",
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "HL7_BATCH",
    "organizationName" : "ignore",
    "topic" : "covid-19",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "az/az-covid-19-hl7",
        "format" : "HL7_BATCH",
        "useBatching" : false,
        "defaults" : { },
        "nameFormat" : "STANDARD",
        "receivingOrganization" : null,
        "type" : "CUSTOM"
    },
    "jurisdictionalFilter" : [ "matches(ordering_facility_county, HL7_BATCH)", "matches(ordering_facility_state, IG)" ],
    "qualityFilter" : [ ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ ],
    "reverseTheQualityFilter" : false,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : {
        "operation" : "MERGE",
        "numberPerDay" : 1440,
        "initialTime" : "00:00",
        "timeZone" : "EASTERN",
        "maxReportCount" : 100,
        "whenEmpty" : {
            "action" : "NONE",
            "onlyOncePerDay" : false
        }
    },
    "description" : "",
    "transport" : {
        "host" : "172.17.6.20",
        "port" : "22",
        "filePath" : "./upload",
        "credentialName" : null,
        "type" : "SFTP"
    },
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : null,
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "FULL_ELR_FHIR",
    "organizationName" : "ignore",
    "topic" : "full-elr",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "",
        "useBatching" : true,
        "nameFormat" : "standard",
        "receivingOrganization" : null,
        "type" : "FHIR"
    },
    "jurisdictionalFilter" : [ "(Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state.exists() and Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state = 'IG') or (Bundle.entry.resource.ofType(Patient).address.state.exists() and Bundle.entry.resource.ofType(Patient).address.state = 'IG')" ],
    "qualityFilter" : [ "Bundle.entry.resource.ofType(MessageHeader).id.exists()", "Bundle.entry.resource.ofType(Patient).birthDate.exists()", "Bundle.entry.resource.ofType(Patient).name.family.exists()", "Bundle.entry.resource.ofType(Patient).name.given.count() > 0", "Bundle.entry.resource.ofType(Specimen).type.exists()", "(Bundle.entry.resource.ofType(Patient).address.line.exists() or Bundle.entry.resource.ofType(Patient).address.postalCode.exists() or Bundle.entry.resource.ofType(Patient).telecom.exists())", "((Bundle.entry.resource.ofType(Specimen).collection.collectedPeriod.exists() or Bundle.entry.resource.ofType(Specimen).collection.collected.exists()) or Bundle.entry.resource.ofType(ServiceRequest).occurrence.exists() or Bundle.entry.resource.ofType(Observation).effective.exists())" ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ "Bundle.entry.resource.ofType(MessageHeader).extension('https://reportstream.cdc.gov/fhir/StructureDefinition/sender-id').exists().not() or Bundle.entry.resource.ofType(MessageHeader).extension('https://reportstream.cdc.gov/fhir/StructureDefinition/sender-id').value != 'hcintegrations'" ],
    "reverseTheQualityFilter" : false,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : {
        "operation" : "MERGE",
        "numberPerDay" : 1440,
        "initialTime" : "00:00",
        "timeZone" : "EASTERN",
        "maxReportCount" : 100,
        "whenEmpty" : {
            "action" : "NONE",
            "onlyOncePerDay" : false
        }
    },
    "description" : "",
    "transport" : {
        "host" : "172.17.6.20",
        "port" : "22",
        "filePath" : "./upload",
        "credentialName" : null,
        "type" : "SFTP"
    },
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : "Ignore FULL_ELR_FHIR",
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "FULL_ELR",
    "organizationName" : "ignore",
    "topic" : "full-elr",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "classpath:/metadata/hl7_mapping/receivers/STLTs/CO/CO.yml",
        "useTestProcessingMode" : false,
        "useBatchHeaders" : true,
        "receivingApplicationName" : "CA-ELR",
        "receivingApplicationOID" : null,
        "receivingFacilityName" : "CA",
        "receivingFacilityOID" : null,
        "messageProfileId" : null,
        "replaceValue" : { },
        "replaceValueAwithB" : { },
        "reportingFacilityName" : null,
        "reportingFacilityId" : null,
        "reportingFacilityIdType" : null,
        "suppressQstForAoe" : false,
        "suppressHl7Fields" : null,
        "suppressAoe" : false,
        "defaultAoeToUnknown" : false,
        "replaceUnicodeWithAscii" : false,
        "useBlankInsteadOfUnknown" : null,
        "truncateHDNamespaceIds" : false,
        "truncateHl7Fields" : null,
        "usePid14ForPatientEmail" : false,
        "convertTimestampToDateTime" : null,
        "cliaForOutOfStateTesting" : null,
        "cliaForSender" : { },
        "phoneNumberFormatting" : "STANDARD",
        "suppressNonNPI" : false,
        "processingModeCode" : null,
        "replaceDiiWithOid" : null,
        "applyOTCDefault" : false,
        "useOrderingFacilityName" : "STANDARD",
        "valueSetOverrides" : { },
        "nameFormat" : "standard",
        "receivingOrganization" : null,
        "convertPositiveDateTimeOffsetToNegative" : false,
        "stripInvalidCharsRegex" : null,
        "convertDateTimesToReceiverLocalTime" : false,
        "useHighPrecisionHeaderDateTimeFormat" : false,
        "type" : "HL7",
        "truncationConfig" : {
            "truncateHDNamespaceIds" : false,
            "truncateHl7Fields" : [ ],
            "customLengthHl7Fields" : { }
        }
    },
    "jurisdictionalFilter" : [ "(Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state.exists() and Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state = 'IG') or (Bundle.entry.resource.ofType(Patient).address.state.exists() and Bundle.entry.resource.ofType(Patient).address.state = 'IG')" ],
    "qualityFilter" : [ "Bundle.entry.resource.ofType(MessageHeader).id.exists()", "Bundle.entry.resource.ofType(Patient).birthDate.exists()", "Bundle.entry.resource.ofType(Patient).name.family.exists()", "Bundle.entry.resource.ofType(Patient).name.given.count() > 0", "Bundle.entry.resource.ofType(Specimen).type.exists()", "(Bundle.entry.resource.ofType(Patient).address.line.exists() or Bundle.entry.resource.ofType(Patient).address.postalCode.exists() or Bundle.entry.resource.ofType(Patient).telecom.exists())", "((Bundle.entry.resource.ofType(Specimen).collection.collectedPeriod.exists() or Bundle.entry.resource.ofType(Specimen).collection.collected.exists()) or Bundle.entry.resource.ofType(ServiceRequest).occurrence.exists() or Bundle.entry.resource.ofType(Observation).effective.exists())" ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ "Bundle.entry.resource.ofType(MessageHeader).extension('https://reportstream.cdc.gov/fhir/StructureDefinition/sender-id').exists().not() or Bundle.entry.resource.ofType(MessageHeader).extension('https://reportstream.cdc.gov/fhir/StructureDefinition/sender-id').value != 'hcintegrations'" ],
    "reverseTheQualityFilter" : false,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : {
        "operation" : "MERGE",
        "numberPerDay" : 1440,
        "initialTime" : "00:00",
        "timeZone" : "EASTERN",
        "maxReportCount" : 100,
        "whenEmpty" : {
            "action" : "NONE",
            "onlyOncePerDay" : false
        }
    },
    "description" : "",
    "transport" : {
        "host" : "172.17.6.20",
        "port" : "22",
        "filePath" : "./upload",
        "credentialName" : null,
        "type" : "SFTP"
    },
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : "Ignore FULL_ELR",
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}, {
    "name" : "ELR_ELIMS",
    "organizationName" : "ignore",
    "topic" : "elr-elims",
    "customerStatus" : "active",
    "translation" : {
        "schemaName" : "classpath:/metadata/hl7_mapping/ORU_R01/ORU_R01-base.yml",
        "useTestProcessingMode" : false,
        "useBatchHeaders" : true,
        "receivingApplicationName" : null,
        "receivingApplicationOID" : null,
        "receivingFacilityName" : null,
        "receivingFacilityOID" : null,
        "messageProfileId" : null,
        "replaceValue" : { },
        "replaceValueAwithB" : { },
        "reportingFacilityName" : null,
        "reportingFacilityId" : null,
        "reportingFacilityIdType" : null,
        "suppressQstForAoe" : false,
        "suppressHl7Fields" : null,
        "suppressAoe" : false,
        "defaultAoeToUnknown" : false,
        "replaceUnicodeWithAscii" : false,
        "useBlankInsteadOfUnknown" : null,
        "truncateHDNamespaceIds" : false,
        "truncateHl7Fields" : null,
        "usePid14ForPatientEmail" : false,
        "convertTimestampToDateTime" : null,
        "cliaForOutOfStateTesting" : null,
        "cliaForSender" : { },
        "phoneNumberFormatting" : "STANDARD",
        "suppressNonNPI" : false,
        "processingModeCode" : null,
        "replaceDiiWithOid" : null,
        "applyOTCDefault" : false,
        "useOrderingFacilityName" : "STANDARD",
        "valueSetOverrides" : { },
        "nameFormat" : "standard",
        "receivingOrganization" : null,
        "convertPositiveDateTimeOffsetToNegative" : false,
        "stripInvalidCharsRegex" : null,
        "convertDateTimesToReceiverLocalTime" : false,
        "useHighPrecisionHeaderDateTimeFormat" : false,
        "type" : "HL7",
        "truncationConfig" : {
            "truncateHDNamespaceIds" : false,
            "truncateHl7Fields" : [ ],
            "customLengthHl7Fields" : { }
        }
    },
    "jurisdictionalFilter" : [ "(Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state.exists() and Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state = 'IG') or (Bundle.entry.resource.ofType(Patient).address.state.exists() and Bundle.entry.resource.ofType(Patient).address.state = 'IG')" ],
    "qualityFilter" : [ "true" ],
    "routingFilter" : [ ],
    "processingModeFilter" : [ ],
    "reverseTheQualityFilter" : false,
    "deidentify" : false,
    "deidentifiedValue" : "",
    "timing" : {
        "operation" : "MERGE",
        "numberPerDay" : 1440,
        "initialTime" : "00:00",
        "timeZone" : "EASTERN",
        "maxReportCount" : 100,
        "whenEmpty" : {
            "action" : "NONE",
            "onlyOncePerDay" : false
        }
    },
    "description" : "",
    "transport" : {
        "host" : "172.17.6.20",
        "port" : "22",
        "filePath" : "./upload",
        "credentialName" : null,
        "type" : "SFTP"
    },
    "version" : null,
    "createdBy" : null,
    "createdAt" : null,
    "conditionFilter" : [ ],
    "mappedConditionFilter" : [ ],
    "externalName" : "ELIMS Ignore Receiver",
    "enrichmentSchemaNames" : [ ],
    "timeZone" : null,
    "dateTimeFormat" : "OFFSET"
}];

export const MOCK_GET_SENDERS_IGNORE = [ {
    "name" : "ignore-empty",
    "organizationName" : "ignore",
    "format" : "CSV",
    "customerStatus" : "active",
    "schemaName" : "empty",
    "processingType" : "sync",
    "allowDuplicates" : true,
    "senderType" : null,
    "primarySubmissionMethod" : null,
    "topic" : "covid-19",
    "version" : null,
    "createdBy" : null,
    "createdAt" : null
}, {
    "name" : "ignore-simple-report",
    "organizationName" : "ignore",
    "format" : "CSV",
    "customerStatus" : "active",
    "schemaName" : "primedatainput/pdi-covid-19",
    "processingType" : "sync",
    "allowDuplicates" : true,
    "senderType" : null,
    "primarySubmissionMethod" : null,
    "topic" : "covid-19",
    "version" : null,
    "createdBy" : null,
    "createdAt" : null
}, {
    "name" : "ignore-waters",
    "organizationName" : "ignore",
    "format" : "CSV",
    "customerStatus" : "active",
    "schemaName" : "waters/waters-covid-19",
    "processingType" : "sync",
    "allowDuplicates" : true,
    "senderType" : null,
    "primarySubmissionMethod" : null,
    "topic" : "covid-19",
    "version" : null,
    "createdBy" : null,
    "createdAt" : null
}, {
    "name" : "ignore-hl7",
    "organizationName" : "ignore",
    "format" : "HL7",
    "customerStatus" : "active",
    "schemaName" : "hl7/test-covid-19",
    "processingType" : "sync",
    "allowDuplicates" : true,
    "senderType" : null,
    "primarySubmissionMethod" : null,
    "topic" : "covid-19",
    "version" : null,
    "createdBy" : null,
    "createdAt" : null
}, {
    "name" : "ignore-full-elr",
    "organizationName" : "ignore",
    "format" : "HL7",
    "customerStatus" : "active",
    "schemaName" : "",
    "processingType" : "sync",
    "allowDuplicates" : true,
    "senderType" : null,
    "primarySubmissionMethod" : null,
    "topic" : "full-elr",
    "version" : null,
    "createdBy" : null,
    "createdAt" : null
}, {
    "name" : "default",
    "organizationName" : "ignore",
    "format" : "CSV",
    "customerStatus" : "active",
    "schemaName" : "primedatainput/pdi-covid-19",
    "processingType" : "sync",
    "allowDuplicates" : true,
    "senderType" : null,
    "primarySubmissionMethod" : null,
    "topic" : "covid-19",
    "version" : null,
    "createdBy" : null,
    "createdAt" : null
}, {
    "name" : "ignore-elr-elims",
    "organizationName" : "ignore",
    "format" : "HL7",
    "customerStatus" : "active",
    "schemaName" : "classpath:/metadata/fhir_transforms/senders/original-pipeline-transforms.yml",
    "processingType" : "sync",
    "allowDuplicates" : true,
    "senderType" : null,
    "primarySubmissionMethod" : null,
    "topic" : "elr-elims",
    "version" : null,
    "createdBy" : null,
    "createdAt" : null
}, {
    "name" : "ignore-full-elr-e2e",
    "organizationName" : "ignore",
    "format" : "HL7",
    "customerStatus" : "active",
    "schemaName" : "classpath:/metadata/fhir_transforms/senders/baseline-sender-transform.yml",
    "processingType" : "sync",
    "allowDuplicates" : true,
    "senderType" : null,
    "primarySubmissionMethod" : null,
    "topic" : "full-elr",
    "version" : null,
    "createdBy" : null,
    "createdAt" : null
} ]

export const MOCK_GET_ORGANIZATION_IGNORE = {
    "name" : "ignore",
    "description" : "FOR TESTING ONLY",
    "jurisdiction" : "FEDERAL",
    "filters" : [ {
        "topic" : "covid-19",
        "jurisdictionalFilter" : [ "matches(ordering_facility_state, IG)" ],
        "qualityFilter" : null,
        "routingFilter" : null,
        "processingModeFilter" : null,
        "conditionFilter" : null,
        "mappedConditionFilter" : null
    }, {
        "topic" : "monkeypox",
        "jurisdictionalFilter" : [ "matches(ordering_facility_state, IG)" ],
        "qualityFilter" : null,
        "routingFilter" : null,
        "processingModeFilter" : null,
        "conditionFilter" : null,
        "mappedConditionFilter" : null
    }, {
        "topic" : "full-elr",
        "jurisdictionalFilter" : [ "(Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state.exists() and Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state = 'IG') or (Bundle.entry.resource.ofType(Patient).address.state.exists() and Bundle.entry.resource.ofType(Patient).address.state = 'IG')" ],
        "qualityFilter" : [ "(Bundle.entry.resource.ofType(MessageHeader).event.code.exists() and Bundle.entry.resource.ofType(MessageHeader).event.code = 'R01')" ],
        "routingFilter" : null,
        "processingModeFilter" : null,
        "conditionFilter" : null,
        "mappedConditionFilter" : null
    }, {
        "topic" : "mars-otc-elr",
        "jurisdictionalFilter" : [ "Bundle.entry.resource.ofType(Patient).address.state.exists() and Bundle.entry.resource.ofType(Patient).address.state = 'IG'" ],
        "qualityFilter" : null,
        "routingFilter" : null,
        "processingModeFilter" : null,
        "conditionFilter" : null,
        "mappedConditionFilter" : null
    } ],
    "featureFlags" : [ ],
    "keys" : [ {
        "scope" : "ignore.*.admin",
        "keys" : [ {
            "kty" : "EC",
            "kid" : "adminkey",
            "crv" : "P-384",
            "x" : "78eOugxhQPd_tUKOhsfcZ04bp_xgL2kuJN6ZrNgWv6qZXHXqoqVKVXlzO_Q9NXdn",
            "y" : "Zo7eBcyQpAarTANsPKB95xT69Ue_cCyp1DBmTRk3nJBBhF6XZkT-AaYaXmGhPNWG"
        } ]
    }, {
        "scope" : "ignore.*.report",
        "keys" : [ ]
    } ],
    "version" : 2197,
    "createdBy" : "ignore.*.admin_8e376400-dd3d-452c-973b-ea38358c4b7d",
    "createdAt" : "2024-09-16T15:18:36.062Z"
}

export const MOCK_GET_ORGANIZATION_SETTINGS_LIST = [
    {
        name: "waters",
        description: "Test Sender from Waters",
        jurisdiction: "FEDERAL",
        filters: [],
        featureFlags: [],
        keys: [],
    },
    {
        name: "ignore",
        description: "FOR TESTING ONLY",
        jurisdiction: "FEDERAL",
        filters: [
            {
                topic: "covid-19",
                jurisdictionalFilter: ["matches(ordering_facility_state, IG)"],
                qualityFilter: null,
                routingFilter: null,
                processingModeFilter: null,
                conditionFilter: null,
                mappedConditionFilter: null,
            },
            {
                topic: "monkeypox",
                jurisdictionalFilter: ["matches(ordering_facility_state, IG)"],
                qualityFilter: null,
                routingFilter: null,
                processingModeFilter: null,
                conditionFilter: null,
                mappedConditionFilter: null,
            },
            {
                topic: "full-elr",
                jurisdictionalFilter: [
                    "(Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state.exists() and Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state = 'IG') or (Bundle.entry.resource.ofType(Patient).address.state.exists() and Bundle.entry.resource.ofType(Patient).address.state = 'IG')",
                ],
                qualityFilter: [
                    "(Bundle.entry.resource.ofType(MessageHeader).event.code.exists() and Bundle.entry.resource.ofType(MessageHeader).event.code = 'R01')",
                ],
                routingFilter: null,
                processingModeFilter: null,
                conditionFilter: null,
                mappedConditionFilter: null,
            },
        ],
        featureFlags: [],
        keys: [],
    },
    {
        name: "historytest",
        description: "FOR TESTING SUBMISSION HISTORY",
        jurisdiction: "FEDERAL",
        filters: [],
        featureFlags: [],
        keys: [],
    },
    {
        name: "simple_report",
        description: "PRIME's POC testing app",
        jurisdiction: "FEDERAL",
        filters: [],
        featureFlags: [],
        keys: [],
    },
    {
        name: "hcintegrations",
        description: "Healthcare Integrations",
        jurisdiction: "FEDERAL",
        filters: [],
        featureFlags: [],
        keys: [],
    },
    {
        name: "oh-doh",
        description: "Ohio Department of Health",
        jurisdiction: "STATE",
        stateCode: "OH",
        filters: [],
        featureFlags: [],
        keys: [],
    },
    {
        name: "hi-phd",
        description: "Hawaii Public Health Department",
        jurisdiction: "STATE",
        stateCode: "HI",
        filters: [],
        featureFlags: [],
        keys: [],
    },
    {
        name: "ak-phd",
        description: "Alaska Public Health Department",
        jurisdiction: "STATE",
        stateCode: "AK",
        filters: [],
        featureFlags: [],
        keys: [],
    },
    {
        name: "md-phd",
        description: "TEST Maryland Public Health Department",
        jurisdiction: "STATE",
        stateCode: "MD",
        filters: [
            {
                topic: "covid-19",
                jurisdictionalFilter: [
                    "orEquals(ordering_facility_state, MD, patient_state, MD)",
                ],
                qualityFilter: null,
                routingFilter: null,
                processingModeFilter: null,
                conditionFilter: null,
                mappedConditionFilter: null,
            },
            {
                topic: "full-elr",
                jurisdictionalFilter: [
                    "(Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state.exists() and Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state = 'MD') or (Bundle.entry.resource.ofType(Patient).address.state.exists() and Bundle.entry.resource.ofType(Patient).address.state = 'MD')",
                ],
                qualityFilter: null,
                routingFilter: null,
                processingModeFilter: null,
                conditionFilter: null,
                mappedConditionFilter: null,
            },
        ],
        featureFlags: [],
        keys: [],
    },
    {
        name: "development",
        description: "FOR DEVELOPMENT PURPOSES ONLY",
        jurisdiction: "FEDERAL",
        filters: [],
        featureFlags: [],
        keys: [],
    },
];
