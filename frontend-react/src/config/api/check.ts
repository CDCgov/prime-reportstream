import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

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
