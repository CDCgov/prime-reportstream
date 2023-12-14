import { HTTPMethods, RSApiEndpoints, RSEndpoint } from "./index";

/* All enums are case sensitive and created to match 1:1
 * with an enum class in the prime-router project. */
export enum Jurisdiction {
    FEDERAL = "FEDERAL",
    STATE = "STATE",
    COUNTY = "COUNTY",
}

// TODO: Consolidate with FileType in UseFileHandler.ts
export enum Format {
    CSV = "CSV",
    HL7 = "HL7",
    FHIR = "FHIR",
}

export enum FileType {
    "CSV" = "CSV",
    "HL7" = "HL7",
}

export enum ContentType {
    "CSV" = "text/csv",
    "HL7" = "application/hl7-v2",
}

export enum CustomerStatus {
    INACTIVE = "inactive",
    TESTING = "testing",
    ACTIVE = "active",
}

export enum ProcessingType {
    SYNC = "sync",
    ASYNC = "async",
}

export enum ReportStreamFilterDefinition {
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

export enum BatchOperation {
    NONE = "NONE",
    MERGE = "MERGE",
}

export enum EmptyOperation {
    NONE = "NONE",
    SEND = "SEND",
}

export enum USTimeZone {
    ARIZONA = "ARIZONA",
    CENTRAL = "CENTRAL",
    CHAMORRO = "CHAMORRO",
    EASTERN = "EASTERN",
    EAST_INDIANA = "EAST_INDIANA",
    HAWAII = "HAWAII",
    INDIANA_STARKE = "INDIANA_STARKE",
    MICHIGAN = "MICHIGAN",
    MOUNTAIN = "MOUNTAIN",
    PACIFIC = "PACIFIC",
    SAMOA = "SAMOA",
    UTC = "UTC",
}

export enum DateTimeFormat {
    DATE_ONLY = "DATE_ONLY",
    HIGH_PRECISION_OFFSET = "HIGH_PRECISION_OFFSET",
    LOCAL = "LOCAL",
    OFFSET = "OFFSET",
}

export enum GAENUUIDFormat {
    PHONE_DATE = "PHONE_DATE",
    REPORT_ID = "REPORT_ID",
    WA_NOTIFY = "WA_NOTIFY",
}

export enum ServicesUrls {
    SETTINGS = "/settings/organizations/:orgId",
    SENDERS = "/settings/organizations/:orgId/senders",
    SENDER_DETAIL = "/settings/organizations/:orgId/senders/:entityId",
    RECEIVERS = "/settings/organizations/:orgId/receivers",
    PUBLIC_KEYS = "/settings/organizations/:orgId/public-keys",
}

export interface Timing {
    initialTime: string; //"00:00"
    maxReportCount: number; //365;
    numberPerDay: number; // = 1;
    operation: BatchOperation;
    timeZone: USTimeZone;
    whenEmpty: {
        action: EmptyOperation;
        onlyOncePerDay: boolean;
    };
}

export interface Translation {
    defaults: Record<string, string>;
    format: Format;
    nameFormat: string;
    receivingOrganization: string;
    schemaName: string;
}

export interface IRsService {
    customerStatus: CustomerStatus;
    organizationName: string;
    topic: string;
}

export interface RsService extends IRsService, RsSetting {}
export interface RsServiceEdit extends IRsService, RsSettingEdit {}

export interface RsSettingMeta {
    createdAt: string;
    createdBy: string;
    version: number;
}

export interface IRsSetting {
    name: string;
    description?: string;
}

export interface RsSetting extends IRsSetting, RsSettingMeta {}
export interface RsSettingEdit extends IRsSetting {}

export interface IRsOrganization {
    filters: string[];
    jurisdiction: string;
    name: string;
    stateCode?: string;
    countyName?: string;
}

export interface RsOrganization extends IRsOrganization, RsSetting {}

export interface RsOrganizationEdit extends IRsOrganization, RsSettingEdit {}

export interface IRsSender {
    allowDuplicates: boolean;
    format: string;
    keys?: ScopedJsonWebKeySet;
    primarySubmissionMethod?: string;
    processingType: string;
    schemaName: string;
    senderType?: string;
}
export interface RsSender extends IRsSender, RsService {}
export interface RsSenderEdit extends IRsSender, RsServiceEdit {}

export interface IRsReceiver {
    translation: Translation;
    jurisdictionalFilter?: object;
    qualityFilter?: object;
    routingFilter?: object;
    processingModeFilter?: object;
    reverseTheQualityFilter?: boolean;
    conditionFilter?: object;
    deidentify?: boolean;
    deidentifiedValue?: string;
    timing?: Timing;
    timeZone?: string;
    dateTimeFormat?: string;
    transport?: Transport;
    externalName?: string;
}
export interface RsReceiver extends IRsReceiver, RsService {}
export interface RsReceiverEdit extends IRsReceiver, RsServiceEdit {}

export type RsSettingType = RsOrganization | RsReceiver | RsSender;
export type RsSettingEditType =
    | RsOrganizationEdit
    | RsReceiverEdit
    | RsSenderEdit;

export interface ScopedJsonWebKeySet extends RsJsonWebKeySet {
    scope: string;
}

export interface RSApiKeysResponse {
    orgName: string;
    keys: ScopedJsonWebKeySet[];
}

export interface SFTPTransport {
    host: string;
    port: string;
    filePath: string;
    credentialName: string;
}

export interface EmailTransport {
    addresses: string[];
    from: string;
}

export interface BlobStoreTransport {
    storageName: string;
    containerName: string;
}

export interface AS2Transport {
    receiverUrl: string;
    receiverId: string;
    senderId: string;
    senderEmail: string;
    mimeType: string;
    contentDescription: string;
}

export interface GAENTransport {
    apiUrl: string;
    uuidFormat: GAENUUIDFormat;
    uuidIV: string;
}

export type Transport =
    | SFTPTransport
    | EmailTransport
    | BlobStoreTransport
    | AS2Transport
    | GAENTransport;

export const customerStatusChoices = Array.from(Object.values(CustomerStatus));
export const formatChoices = Array.from(Object.values(Format));
export const jurisdictionChoices = Array.from(Object.values(Jurisdiction));
export const processingTypeChoices = Array.from(Object.values(ProcessingType));
export const reportStreamFilterDefinitionChoices = Array.from(
    Object.values(ReportStreamFilterDefinition),
);
export const dateTimeFormatChoices = Array.from(Object.values(DateTimeFormat));
export const usTimeZoneChoices = Array.from(Object.values(USTimeZone));

export const SampleTiming = {
    initialTime: "00:00",
    maxReportCount: 365,
    numberPerDay: 1,
    operation: BatchOperation.MERGE,
    timeZone: USTimeZone.ARIZONA,
    whenEmpty: {
        action: EmptyOperation.NONE,
        onlyOncePerDay: true,
    },
} satisfies Timing;

export const SampleTranslation = {
    defaults: {},
    format: Format.CSV,
    nameFormat: "",
    receivingOrganization: "xx_phd",
    schemaName: "schema",
} satisfies Translation;

export const SampleTransports = {
    SFTP: {
        credentialName: "",
        filePath: "",
        host: "",
        port: "",
    } satisfies SFTPTransport,
    Email: {
        addresses: [""],
        from: "",
    } satisfies EmailTransport,
    BlobStore: {
        containerName: "",
        storageName: "",
    } satisfies BlobStoreTransport,
    AS2: {
        contentDescription: "",
        mimeType: "",
        receiverId: "",
        receiverUrl: "",
        senderEmail: "",
        senderId: "",
    } satisfies AS2Transport,
    GAEN: {
        apiUrl: "",
        uuidFormat: GAENUUIDFormat.PHONE_DATE,
        uuidIV: "",
    } satisfies GAENTransport,
};

export const SampleScopedJwks = {
    scope: "scope",
    keys: [
        {
            kty: "RSA",
            n: "",
            e: "",
        } satisfies RSAPublicJsonWebKey,
    ],
} satisfies ScopedJsonWebKeySet;

/*
Services Endpoints
* senders -> fetches a list of organization's senders
* receivers -> fetches a list of organization's receivers
*/
export const servicesEndpoints: RSApiEndpoints = {
    settings: new RSEndpoint({
        path: ServicesUrls.SETTINGS,
        method: HTTPMethods.GET,
        queryKey: "servicesSettings",
    }),
    senders: new RSEndpoint({
        path: ServicesUrls.SENDERS,
        method: HTTPMethods.GET,
        queryKey: "servicesSenders",
    }),
    senderDetail: new RSEndpoint({
        path: ServicesUrls.SENDER_DETAIL,
        method: HTTPMethods.GET,
        queryKey: "servicesSenderDetail",
    }),
    receivers: new RSEndpoint({
        path: ServicesUrls.RECEIVERS,
        method: HTTPMethods.GET,
        queryKey: "servicesReceivers",
    }),
    publicKeys: new RSEndpoint({
        path: ServicesUrls.PUBLIC_KEYS,
        method: HTTPMethods.GET,
        queryKey: "publicKeys",
    }),
    createPublicKey: new RSEndpoint({
        path: ServicesUrls.PUBLIC_KEYS,
        method: HTTPMethods.POST,
        queryKey: "createPublicKey",
    }),
};
