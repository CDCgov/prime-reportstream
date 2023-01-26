import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

/*
Services Endpoints
* senders -> fetches a list of organization's senders
* receivers -> fetches a list of organization's receivers
*/
export const adminEndpoints = {
    resend: new RSEndpoint({
        path: "/adm/getresend",
        methods: {
            [HTTPMethods.GET]: {} as AdminAction[],
        },
        params: {
            days_to_show: {} as number,
        },
        queryKey: "adminResend",
    } as const),
    sendFailures: new RSEndpoint({
        path: "/adm/getsendfailures",
        methods: {
            [HTTPMethods.GET]: {} as SendFailure[],
        },
        queryKey: "adminSendFailures",
    } as const),
    listReceiversConnectionStatus: new RSEndpoint({
        path: "/adm/listreceiversconnstatus",
        methods: {
            [HTTPMethods.GET]: {} as ReceiverConnectionStatus[],
        },
        queryKey: "adminListReceiversConnectionStatus",
    } as const),
};
