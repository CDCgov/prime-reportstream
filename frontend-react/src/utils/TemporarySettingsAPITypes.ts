enum Jurisdiction {
    FEDERAL = "FEDERAL",
    STATE = "STATE",
    COUNTY = "COUNTY",
}

enum Format {
    CSV = "text/csv",
    HL7 = "application/hl7-v2",
}

enum CustomerStatus {
    INACTIVE = "inactive",
    TESTING = "testing",
    ACTIVE = "active",
}

enum ProcessingType {
    SYNC = "sync",
    ASYNC = "async",
}

/* Alias can be updated with proper RS filter enum
 * once #4353 is complete */
type ReportStreamFilter = Array<string>;

interface SettingMetadata {
    version: number;
    createdBy: string;
    createdAt: string; // ISO string date
}

interface ReportStreamFilters {
    topic: string;
    jurisdictionalFilter?: ReportStreamFilter;
    qualityFilter?: ReportStreamFilter;
    routingFilter?: ReportStreamFilter;
    processingModeFilter?: ReportStreamFilter;
}

interface OrganizationAPI {
    name: string;
    description: string;
    jurisdiction: Jurisdiction;
    stateCode?: string;
    countyName?: string;
    filters?: Array<ReportStreamFilters>;
    meta?: SettingMetadata;
}

interface SenderAPI {
    name: string;
    organizationName: string;
    format: Format;
    topic: string;
    customerStatus: CustomerStatus;
    schemaName: string;
    keys: Array<string>
    processingType: ProcessingType;
    allowDuplicates: boolean;
    meta?: SettingMetadata;
}

interface ReceiverAPI {
    name: string;
    organizationName: string;
    description: string;
    topic: string;
    customerStatus: CustomerStatus;
    // translation: any, (?)
    jurisdictionalFilter: ReportStreamFilters;
    qualityFilter: ReportStreamFilters;
    routingFilter: ReportStreamFilters;
    processingModeFilter: ReportStreamFilters;
    reverseTheQualityFilter: boolean;
    deidentify: boolean;
    // timing: any (?)
    // transportType: any (?)
    meta?: SettingMetadata;
}

export {
    Jurisdiction,
    Format,
    ProcessingType,
    CustomerStatus,
};

export type {
    OrganizationAPI,
    SenderAPI,
    ReceiverAPI,
    SettingMetadata,
    ReportStreamFilter,
    ReportStreamFilters,
};
