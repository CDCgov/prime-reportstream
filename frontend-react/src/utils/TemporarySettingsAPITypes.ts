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

enum ReportStreamFilterDefinition {
    BY_COUNTY = "filterByCounty",
    MATCHES = "matches",
    NO_MATCH = "doesNotMatch",
    EQUALS = "orEquals",
    VALID_DATA = "hasValidDataFor",
    AT_LEAST_ONE = "hasAtLeastOneOf",
    ALLOW_ALL = "allowAll",
    ALLOW_NONE = "allowNone",
    VALID_CLIA = "isValidCLIA",
    DATE_INTERVAL = "inDateInterval",
}

enum BatchOperation {
    NONE = "NONE",
    MERGE = "MERGE",
}

enum EmptyOperation {
    NONE = "NONE",
    SEND = "SEND",
}

enum USTimeZone {
    PACIFIC = "US/Pacific",
    MOUNTAIN = "US/Mountain",
    ARIZONA = "US/Arizona",
    CENTRAL = "US/Central",
    EASTERN = "US/Eastern",
    SAMOA = "US/Samoa",
    HAWAII = "US/Hawaii",
    EAST_INDIANA = "US/East-Indiana",
    INDIANA_STARKE = "US/Indiana-Starke",
    MICHIGAN = "US/Michigan",
    CHAMORRO = "Pacific/Guam",
}

type ReportStreamFilter = Array<ReportStreamFilterDefinition>;

interface Jwk {
    kty: string;
    use?: string;
    keyOps?: string;
    alg?: string;
    x5u?: string;
    x5c?: string;
    x5t?: string; // certificate thumbprint
    // Algorithm specific fields
    n?: string; // RSA
    e?: string; // RSA
    d?: string; // EC and RSA private
    crv?: string; // EC
    p?: string; // RSA private
    q?: string; // RSA private
    dp?: string; // RSA private
    dq?: string; // RSA private
    qi?: string; // RSA private
    x?: string; // EC
    y?: string; // EC
    k?: string; // symmetric key, eg oct
}

interface JwkSet {
    scope: string;
    keys: Array<Jwk>;
}

interface TranslatorProperties {
    format: Format;
    schemaName: string;
    defaults: Map<string, string>;
    nameFormat: string;
    receivingOrganization?: string;
}

interface WhenEmpty {
    action: EmptyOperation;
    onlyOncePerDay: boolean;
}

interface Timing {
    operation: BatchOperation;
    numberPerDay: number;
    initialTime: string;
    timeZone: USTimeZone;
    maxReportCount: number;
    whenEmpty: WhenEmpty;
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
}

interface SenderAPI {
    name: string;
    organizationName: string;
    format: Format;
    topic: string;
    customerStatus: CustomerStatus;
    schemaName: string;
    keys: Array<JwkSet>;
    processingType: ProcessingType;
    allowDuplicates: boolean;
}

interface ReceiverAPI {
    name: string;
    organizationName: string;
    description: string;
    topic: string;
    customerStatus: CustomerStatus;
    translation: TranslatorProperties;
    jurisdictionalFilter: ReportStreamFilters;
    qualityFilter: ReportStreamFilters;
    routingFilter: ReportStreamFilters;
    processingModeFilter: ReportStreamFilters;
    reverseTheQualityFilter: boolean;
    deidentify: boolean;
    timing?: Timing;
    // transportType?: any (?)
}

type ReportStreamFieldType =
    | "string"
    | "boolean"
    | "keys"
    | "filterObj"
    | "translation"
    | "timing";

type ReportStreamSettingsEnum =
    | "jurisdiction"
    | "format"
    | "customerStatus"
    | "reportStreamFilterDefinition";

const getListOfEnumValues = (e: ReportStreamSettingsEnum): string[] => {
    switch (e) {
        case "customerStatus":
            return Array.from(Object.values(CustomerStatus));
        case "format":
            return Array.from(Object.values(Format));
        case "jurisdiction":
            return Array.from(Object.values(Jurisdiction));
        case "reportStreamFilterDefinition":
            return Array.from(Object.values(ReportStreamFilterDefinition));
    }
};

const sampleFilterObj: ReportStreamFilters = {
    jurisdictionalFilter: [],
    processingModeFilter: [],
    qualityFilter: [],
    routingFilter: [],
    topic: "",
};

const sampleKeysObj: Array<JwkSet> = [
    {
        scope: "",
        keys: [
            {
                kty: "",
                use: "",
                keyOps: "",
                alg: "",
                x5u: "",
                x5c: "",
                x5t: "",
                n: "",
                e: "",
                d: "",
                crv: "",
                p: "",
                q: "",
                dp: "",
                dq: "",
                qi: "",
                x: "",
                y: "",
                k: "",
            },
        ],
    },
];

const sampleTimingObj: Timing = {
    initialTime: "00:00",
    maxReportCount: 365,
    numberPerDay: 1,
    operation: BatchOperation.MERGE,
    timeZone: USTimeZone.ARIZONA,
    whenEmpty: {
        action: EmptyOperation.NONE,
        onlyOncePerDay: true,
    },
};

const sampleTranslationObj: TranslatorProperties = {
    defaults: new Map<string, string>([["", ""]]),
    format: Format.CSV,
    nameFormat: "",
    receivingOrganization: "xx_phd",
    schemaName: "schema",
};

const getSampleValue = (field: ReportStreamFieldType): string => {
    switch (field) {
        case "boolean":
            return "true or false";
        case "string":
            return '"a string"';
        case "filterObj":
            return JSON.stringify(sampleFilterObj);
        case "keys":
            return JSON.stringify(sampleKeysObj);
        case "timing":
            return JSON.stringify(sampleTimingObj);
        case "translation":
            return JSON.stringify(sampleTranslationObj);
    }
};

export {
    Jurisdiction,
    Format,
    ProcessingType,
    CustomerStatus,
    getListOfEnumValues,
    getSampleValue,
};

export type {
    OrganizationAPI,
    SenderAPI,
    ReceiverAPI,
    ReportStreamFilter,
    ReportStreamFilters,
};
