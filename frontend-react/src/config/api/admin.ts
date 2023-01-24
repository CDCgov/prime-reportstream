import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

export interface AdminAction {
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
export type ReceiverConnectionStatus = {
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

export interface SendFailure {
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

/*
Services Endpoints
* senders -> fetches a list of organization's senders
* receivers -> fetches a list of organization's receivers
*/
export const adminEndpoints = {
    resend: new RSEndpoint({
        path: "/adm/getresend",
        methods: {
            [HTTPMethods.GET]: {} as unknown,
        },
        queryKey: "adminResend",
    } as const),
    sendFailures: new RSEndpoint({
        path: "/adm/getsendfailures",
        methods: {
            [HTTPMethods.GET]: {} as unknown,
        },
        queryKey: "adminSendFailures",
    } as const),
    listReceiversConnectionStatus: new RSEndpoint({
        path: "/adm/listreceiversconnstatus",
        methods: {
            [HTTPMethods.GET]: {} as unknown,
        },
        queryKey: "adminListReceiversConnectionStatus",
    } as const),
};
