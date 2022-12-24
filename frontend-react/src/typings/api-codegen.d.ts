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