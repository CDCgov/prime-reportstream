export enum LookupTables {
    VALUE_SET = "sender_automation_value_set",
    VALUE_SET_ROW = "sender_automation_value_set_row",
}

export enum OverallStatus {
    VALID = "Valid",
    ERROR = "Error",
    WAITING_TO_DELIVER = "Waiting to Deliver",
}

/*
The error codes map to the error types specified in the serializer
 */
export enum ErrorCodeTranslation {
    INVALID_HL7_MSG_VALIDATION = "Invalid entry for field.",
    INVALID_MSG_PARSE_DATE = "Invalid entry for field. Reformat to either the HL7 v2.4 TS or ISO 8601 standard format.",
    INVALID_HL7_MSG_FORMAT_INVALID = "",
    INVALID_MSG_PARSE_TELEPHONE = "The string supplied is not a valid phone number. Reformat to a 10-digit phone number (e.g. (555) 555-5555).",
    UNKNOWN = "",
}

declare global {
    /**
     * ADMIN
     */

    interface AdminAction {
        actionId: number; // BigInt
        actionName: string;
        createdAt: string;
        httpStatus: number;
        actionParams: string;
        actionResult: string;
        actionResponse: string;
        contentLength: number;
        senderIp: string;
        sendingOrg: string;
        sendingOrgClient: string;
        externalName: string;
        username: string;
    }

    /** having the type separate makes unit tests easier **/
    type ReceiverConnectionStatus = {
        /* the unique id  */
        receiverConnectionCheckResultId: number;
        organizationId: number;
        receiverId: number;
        connectionCheckResult: string;
        connectionCheckSuccessful: boolean;
        connectionCheckStartedAt: string;
        connectionCheckCompletedAt: string;
        organizationName: string;
        receiverName: string;
    };

    interface SendFailure {
        /* the unique id for the action */
        actionId: number;
        /* the uuid for this report */
        reportId: string;
        /* Org destination name of the receiver that failed */
        receiver: string;
        /* Filename for the data that's prepared for forwarding but failing */
        fileName: string;
        /* the time that the particular error happened */
        failedAt: string;
        /* The original action that failed had a url. These are the cgi params. */
        actionParams: string;
        /* The long error message generated when the upload failed. */
        actionResult: string;
        /* The body portion of the original action url. Contains the location of the file that failed to forward */
        bodyUrl: string;
        /* The parsed receiver. It should be the same as receiver field above */
        reportFileReceiver: string;
    }

    /**
     * CHECK
     */

    /**
     * See prime-router/docs/api/check.yml for documentation
     */
    interface CheckSettingResult {
        result: "success" | "fail" | ""; // "" is client-side uninitialized state
        message: string;
    }

    /** parameters used for the request. Also used by the react page to make passing data down easier **/
    type CheckSettingParams = {
        orgName: string;
        receiverName: string;
    };

    /**
     * DELIVERY
     */
    interface RSDelivery {
        deliveryId: number;
        batchReadyAt: string;
        expires: string;
        receiver: string;
        reportId: string;
        topic: string;
        reportItemCount: number;
        fileName: string;
        fileType: string;
    }

    interface RSFacility {
        facility: string | undefined;
        location: string | undefined;
        CLIA: string | undefined;
        positive: number | undefined;
        total: number | undefined;
    }

    interface Destination {
        organization_id: string;
        organization: string;
        service: string;
        filteredReportRows: string[];
        filteredReportItems: FilteredReportItem[];
        sending_at: string;
        itemCount: number;
        itemCountBeforeQualityFiltering: number;
        sentReports: string[];
    }

    interface FilteredReportItem {
        filterType: string;
        filterName: string;
        filteredTrackingElement: string;
        filterArgs: string[];
        message: string;
    }

    interface ReportWarning {
        scope: string;
        errorCode: string;
        type: string;
        message: string;
    }

    interface ReportError extends ReportWarning {
        index: number;
        trackingId: string;
    }

    type OrganizationSubmissionsParams = {
        organization: string;
        sortdir: string;
        sortcol: string;
        cursor: string;
        since: string;
        until: string;
        pageSize: number;
        showFailed: boolean;
    };

    interface OrganizationSubmission {
        submissionId: number;
        timestamp: string; // format is "2022-02-01T15:11:58.200754Z"
        sender: string;
        httpStatus: number;
        externalName: string;
        id: string | undefined;
        topic: string;
        reportItemCount: number;
        warningCount: number;
        errorCount: number;
    }

    interface ActionDetail {
        submissionId: number;
        timestamp: string; //comes in format yyyy-mm-ddThh:mm:ss.sssZ
        sender: string | undefined;
        httpStatus: number;
        externalName: string;
        id: string;
        destinations: Destination[];
        errors: ReportError[];
        warnings: ReportWarning[];
        topic: string;
        warningCount: number;
        errorCount: number;
    }

    /**
     * HISTORY
     */

    interface HistorySummaryTest {
        id: string;
        title: string;
        subtitle: string;
        daily: number;
        last: number;
        positive: boolean;
        change: number;
        data: number[];
    }

    interface HistoryReportFacility {
        facility: string | undefined;
        location: string | undefined;
        CLIA: string | undefined;
        positive: string | undefined;
        total: string | undefined;
    }

    interface HistoryReport {
        sent: number;
        via: string;
        // positive: number = 1;
        total: number;
        fileType: string;
        type: string;
        reportId: string;
        expires: number;
        sendingOrg: string;
        receivingOrg: string;
        receivingOrgSvc: string;
        facilities: string[];
        actions: AdminAction[];
        displayName: string;
        content: string;
        fileName: string;
        mimeType: string;
    }
    /**
     * @todo Remove once refactored out of Report download call (when RSDelivery.content exists)
     * @deprecated For compile-time type checks while #5892 is worked on
     */
    interface RSReportInterface {
        batchReadyAt: number;
        via: string;
        positive: number;
        reportItemCount: number;
        fileType: string;
        type: string;
        reportId: string;
        expires: number;
        sendingOrg: string;
        receivingOrg: string;
        receivingOrgSvc: string;
        facilities: string[];
        actions: AdminAction[];
        displayName: string;
        content: string;
        fileName: string;
        mimeType: string;
    }

    interface TestCard {
        id: string;
        title: string;
        subtitle: string;
        daily: number;
        last: number;
        positive: boolean;
        change: number;
        data: number[];
    }

    /**
     * LOOKUP TABLES
     */

    // the shape used by the frontend client for value sets
    interface ValueSet {
        name: string;
        createdBy: string;
        createdAt: string;
        system: string;
    }

    // the shape sent down by the API for value sets
    interface ApiValueSet {
        name: string;
        created_by: string; // unused
        created_at: string; // unused
        system: string;
        reference: string; // unused
        referenceURL: string; // unused
    }

    interface ValueSetRow {
        name: string;
        code: string;
        display: string;
        version: string;
    }

    interface LookupTable {
        lookupTableVersionId: number;
        tableName: string;
        tableVersion: number;
        isActive: boolean;
        createdBy: string;
        createdAt: string;
        tableSha256Checksum: string;
    }

    interface QualityFilter {
        trackingId: string | null;
        detail: any;
    }

    interface ReceiverData {
        reportId: string;
        receivingOrg: string;
        receivingOrgSvc: string;
        transportResult: string | null;
        fileName: string | null;
        fileUrl: string | null;
        createdAt: string;
        qualityFilters: QualityFilter[];
    }

    interface WarningError {
        detail: WarningErrorDetail;
    }

    interface WarningErrorDetail {
        class: string | null;
        fieldMapping: string | null;
        scope: string | null;
        message: string | null;
    }

    /**
     * MESSAGE TRACKER
     */
    interface MessagesItem {
        messageId: string;
        sender: string | undefined;
        submittedDate: string | undefined;
        reportId: string;
    }

    interface Message extends MessagesItem {
        id: number;
        fileName: string | null;
        fileUrl: string | null;
        warnings: WarningError[];
        errors: WarningError[];
        receiverData: ReceiverData[];
    }

    /**
     * SETTING REVISIONS
     */

    interface MetaTaggedResource {
        version: number;
        createdBy: string;
        createdAt: string;
    }

    /** shape of data returned **/
    interface SettingRevision extends MetaTaggedResource {
        id: number;
        name: string;
        isDeleted: boolean;
        isActive: boolean;
        settingJson: string;
    }

    /** parameters used for the request. Also used by the react page to make passing data down easier **/
    type SettingRevisionParams = {
        org: string;
        settingType: "sender" | "receiver" | "organization";
    };

    /**
     * SETTINGS
     */

    /** Response is much larger than this but not all of it is used for front-end yet */
    interface RSService extends MetaTaggedResource {
        name: string;
        organizationName: string;
        topic?: string;
        customerStatus?: string;
    }

    interface RSOrganizationSettings extends RSService {
        description: string;
        filters: string[];
        jurisdiction: string;
        name: string;
        stateCode?: string;
        countyName?: string;
    }

    interface SenderKeys {
        scope: string;
        keys: {}[];
    }

    interface RSSender extends RSOrganizationSettings {
        allowDuplicates: boolean;
        customerStatus: string;
        format: string;
        keys?: SenderKeys | SenderKeys[];
        name: string;
        organizationName: string;
        primarySubmissionMethod?: string;
        processingType: string;
        schemaName: string;
        senderType?: string;
        topic: string;
    }

    interface RSReceiver extends RSOrganizationSettings {
        organizationName: string;
        topic: string;
        customerStatus: string;
        translation: object;
        description: string;
        jurisdictionalFilter: object;
        qualityFilter: object;
        routingFilter: object;
        processingModeFilter: object;
        reverseTheQualityFilter: boolean;
        deidentify: boolean;
        timing: object;
        transport: object;
        externalName: string;
    }

    /**
     * WATERS
     */

    /*
  shape of response from the Waters API
  @todo refactor to move away from all of these optional fields. Which of these are actually optional?
*/
    class WatersResponse {
        actualCompletionAt?: string;
        destinationCount?: number;
        destinations?: Destination[];
        errorCount?: number;
        errors?: ResponseError[];
        externalName?: string;
        httpStatus?: number;
        id?: string;
        overallStatus?: OverallStatus;
        plannedCompletionAt?: string;
        reportItemCount?: number;
        sender?: string;
        submissionId?: number;
        timestamp?: string;
        topic?: string;
        warningCount?: number;
        warnings?: ResponseError[];
        ok?: boolean;
        status?: number;
    }

    interface ResponseError {
        field: string | undefined;
        indices: number[] | undefined;
        message: string | undefined;
        scope: string | undefined;
        errorCode: keyof typeof ErrorCodeTranslation;
        trackingIds: string[] | undefined;
        details: any | undefined;
        rowList?: string;
    }
}
