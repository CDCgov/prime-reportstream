import { RSEndpoint, HTTPMethods } from ".";

export const checkSettingsEndpoints = {
    checkReceiver: new RSEndpoint({
        path: "/checkreceiver/org/:orgName/receiver/:receiverName",
        method: HTTPMethods.POST,
    }),
};
