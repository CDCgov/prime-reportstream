/* All enums are case sensitive and created to match 1:1
 * with an enum class in the prime-router project. */

enum Jurisdiction {
    FEDERAL = "FEDERAL",
    STATE = "STATE",
    COUNTY = "COUNTY",
}

// TODO: Consolidate with FileType in UseFileHandler.ts
enum Format {
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

enum DateTimeFormat {
    DATE_ONLY = "DATE_ONLY",
    HIGH_PRECISION_OFFSET = "HIGH_PRECISION_OFFSET",
    LOCAL = "LOCAL",
    OFFSET = "OFFSET",
}

enum GAENUUIDFormat {
    PHONE_DATE = "PHONE_DATE",
    REPORT_ID = "REPORT_ID",
    WA_NOTIFY = "WA_NOTIFY",
}

type ReportStreamSettingsEnum =
    | "jurisdiction"
    | "format"
    | "customerStatus"
    | "processingType"
    | "reportStreamFilterDefinition"
    | "dateTimeFormat"
    | "timeZone";

const getListOfEnumValues = (e: ReportStreamSettingsEnum): string[] => {
    switch (e) {
        case "customerStatus":
            return Array.from(Object.values(CustomerStatus));
        case "format":
            return Array.from(Object.values(Format));
        case "jurisdiction":
            return Array.from(Object.values(Jurisdiction));
        case "processingType":
            return Array.from(Object.values(ProcessingType));
        case "reportStreamFilterDefinition":
            return Array.from(Object.values(ReportStreamFilterDefinition));
        case "dateTimeFormat":
            return Array.from(Object.values(DateTimeFormat));
        case "timeZone":
            return Array.from(Object.values(USTimeZone));
    }
};

abstract class SampleObject {
    stringify() {
        return JSON.stringify(this, null, 6);
    }
    abstract getAllEnums(): Map<string, string[]>;
    abstract description(): string;
}

class SampleFilterObject extends SampleObject {
    filters = [
        {
            topic: "covid-19",
            jurisdictionalFilter: [],
            qualityFilter: [],
            routingFilter: [],
            processingModeFilter: [],
        },
    ];

    stringify(): string {
        return JSON.stringify(this.filters, null, 6);
    }

    getAllEnums(): Map<string, string[]> {
        return new Map<string, string[]>([
            [
                "Available Filters",
                Array.from(Object.values(ReportStreamFilterDefinition)),
            ],
        ]);
    }

    description(): string {
        return "This field takes an array of filter objects (see object above). Click this tooltip to copy the sample value.";
    }
}

class SampleJwkSet {
    scope = "scope";
    keys = {
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
    };
}

class SampleKeysObj extends SampleObject {
    listOfKeys = [new SampleJwkSet()];
    stringify(): string {
        return JSON.stringify(this.listOfKeys, null, 6);
    }
    getAllEnums(): Map<string, string[]> {
        return new Map(); // Currently doesn't require any enums
    }
    description(): string {
        return "This field takes an array of JwkSets (see above). Click this tooltip to copy the sample value.";
    }
}

class SampleTimingObj extends SampleObject {
    initialTime = "00:00";
    maxReportCount = 365;
    numberPerDay = 1;
    operation = BatchOperation.MERGE;
    timeZone = USTimeZone.ARIZONA;
    whenEmpty = {
        action: EmptyOperation.NONE,
        onlyOncePerDay: true,
    };

    getAllEnums(): Map<string, string[]> {
        return new Map<string, string[]>([
            ["operation", Array.from(Object.values(BatchOperation))],
            ["timeZone", Array.from(Object.values(USTimeZone))],
            ["whenEmpty.action", Array.from(Object.values(EmptyOperation))],
        ]);
    }

    description(): string {
        return "This field takes a timing object (see above). Click this tooltip to copy the sample value.";
    }
}

class SampleTranslationObj extends SampleObject {
    defaults = new Map<string, string>([["", ""]]);
    format = Format.CSV;
    nameFormat = "";
    receivingOrganization = "xx_phd";
    schemaName = "schema";

    getAllEnums(): Map<string, string[]> {
        return new Map<string, string[]>([
            ["format", Array.from(Object.values(Format))],
        ]);
    }

    description(): string {
        return "This field takes a translation object (see above). Click this tooltip to copy the sample value.";
    }
}

class SampleTransportObject extends SampleObject {
    SFTP = {
        host: "",
        port: "",
        filePath: "",
        credentialName: "",
    };

    Email = {
        addresses: [""],
        from: "",
    };

    BlobStore = {
        storageName: "",
        containerName: "",
    };

    AS2TransportType = {
        receiverUrl: "",
        receiverId: "",
        senderId: "",
        senderEmail: "",
        mimeType: "",
        contentDescription: "",
    };

    GAEN = {
        apiUrl: "",
        uuidFormat: GAENUUIDFormat.REPORT_ID,
        uuidIV: "",
    };

    getAllEnums(): Map<string, string[]> {
        return new Map<string, string[]>([
            ["GAEN.uuidFormat", Array.from(Object.values(GAENUUIDFormat))],
        ]);
    }

    description(): string {
        return "This field can take one of these TransportType objects.";
    }
}

export {
    Jurisdiction,
    Format,
    ProcessingType,
    CustomerStatus,
    SampleFilterObject,
    SampleKeysObj,
    SampleTranslationObj,
    SampleTimingObj,
    SampleTransportObject,
    SampleObject,
    getListOfEnumValues,
};

export type { ReportStreamSettingsEnum };
