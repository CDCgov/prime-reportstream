import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

/**
 * See prime-router/docs/api/check.yml for documentation
 */
export interface CheckSettingResult {
    result: "success" | "fail" | ""; // "" is client-side uninitialized state
    message: string;
}

/** parameters used for the request. Also used by the react page to make passing data down easier **/
export type CheckSettingParams = {
    orgName: string;
    receiverName: string;
};

export const checkSettingsCmdEndpoints = {
    checkOrganizationReceiver: new RSEndpoint({
        path: "/checkreceiver/org/:orgName/receiver/:receiverName",
        methods: {
            [HTTPMethods.POST]: {} as CheckSettingResult,
        },
        params: {} as CheckSettingParams,
        queryKey: "checkOrganizationReceiver",
    } as const),
};
