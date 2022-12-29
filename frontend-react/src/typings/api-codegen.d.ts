type Topic = "FULL_ELR" | "COVID_19" | "MONKEYPOX" | "CSV_TESTS" | "TEST";

interface ReportStreamFilters {
    jurisdictionalFilter: string[] | undefined;
    processingModeFilter: string[] | undefined;
    qualityFilter: string[] | undefined;
    routingFilter: string[] | undefined;
    topic: Topic;
}

type Jurisdiction = "FEDERAL" | "STATE" | "COUNTY";

interface Organization {
    countyName: string | undefined;
    description: string;
    filters: ReportStreamFilters[] | undefined;
    jurisdiction: Jurisdiction;
    name: string;
    stateCode: string | undefined;
}

interface TemporalAccessor {
}

interface Temporal extends TemporalAccessor {
}

interface TemporalAdjuster {
}

interface ZoneId {
}

interface ZoneOffset extends ZoneId, TemporalAccessor, TemporalAdjuster {
    totalSeconds: number;
    id: string;
}

interface OffsetDateTime extends Temporal, TemporalAdjuster {
    offset: ZoneOffset;
}

interface SettingAPI {
    createdAt: OffsetDateTime | undefined;
    createdBy: string | undefined;
    name: string;
    organizationName: string | undefined;
    version: number | undefined;
}

interface OrganizationAPI extends Organization, SettingAPI {
    createdAt: OffsetDateTime | undefined;
    createdBy: string | undefined;
    organizationName: undefined;
    version: number | undefined;
}

type CustomerStatus = "INACTIVE" | "TESTING" | "ACTIVE";

type DateTimeFormat = "OFFSET" | "LOCAL" | "HIGH_PRECISION_OFFSET" | "DATE_ONLY";

type USTimeZone = "PACIFIC" | "MOUNTAIN" | "ARIZONA" | "CENTRAL" | "EASTERN" | "SAMOA" | "HAWAII" | "EAST_INDIANA" | "INDIANA_STARKE" | "MICHIGAN" | "CHAMORRO" | "UTC";

type BatchOperation = "NONE" | "MERGE";

type EmptyOperation = "NONE" | "SEND";

interface WhenEmpty {
    action: EmptyOperation;
    onlyOncePerDay: boolean;
}

interface Timing {
    initialTime: string;
    maxReportCount: number;
    numberPerDay: number;
    operation: BatchOperation;
    timeZone: USTimeZone;
    whenEmpty: WhenEmpty;
}

type Format = "INTERNAL" | "CSV" | "CSV_SINGLE" | "HL7" | "HL7_BATCH" | "FHIR";

interface TranslatorProperties {
    defaults: { [key: string]: string };
    format: Format;
    nameFormat: string;
    receivingOrganization: string | undefined;
    schemaName: string;
}

interface TranslatorConfiguration extends TranslatorProperties {
    type: string;
}

interface TransportType {
    type: string;
}

interface Receiver {
    customerStatus: CustomerStatus;
    dateTimeFormat: DateTimeFormat | undefined;
    deidentifiedValue: string;
    deidentify: boolean;
    description: string;
    externalName: string | undefined;
    jurisdictionalFilter: string[];
    name: string;
    organizationName: string;
    processingModeFilter: string[];
    qualityFilter: string[];
    reverseTheQualityFilter: boolean;
    routingFilter: string[];
    timeZone: USTimeZone | undefined;
    timing: Timing | undefined;
    topic: Topic;
    translation: TranslatorConfiguration;
    transport: TransportType | undefined;
}

interface ReceiverAPI extends Receiver, SettingAPI {
    createdAt: OffsetDateTime | undefined;
    createdBy: string | undefined;
    version: number | undefined;
}

interface UUID {
}

interface ListSendFailures {
    actionId: number;
    reportId: UUID;
    receiver: string;
    fileName: string;
    failedAt: OffsetDateTime;
    actionParams: string;
    actionResult: string;
    bodyUrl: string;
    reportFileReceiver: string;
}

interface LookupTableVersion {
    lookupTableVersionId: number;
    tableName: string;
    tableVersion: number;
    isActive: boolean;
    createdBy: string;
    createdAt: OffsetDateTime;
    tableSha256Checksum: string;
}