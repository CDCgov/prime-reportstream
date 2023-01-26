import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

/*
Services Endpoints
* senders -> fetches a list of organization's senders
* receivers -> fetches a list of organization's receivers
*/
export const settingsEndpoints = {
    organizations: new RSEndpoint({
        path: "/settings/organizations",
        methods: {
            [HTTPMethods.GET]: {} as RSOrganizationSettings[],
        },
        queryKey: "settingsOrganizations",
    } as const),
    organization: new RSEndpoint({
        path: "/settings/organizations/:orgName",
        methods: {
            [HTTPMethods.GET]: {} as RSOrganizationSettings,
            [HTTPMethods.POST]: {} as RSOrganizationSettings,
            [HTTPMethods.PUT]: {} as RSOrganizationSettings,
            [HTTPMethods.DELETE]: {} as RSOrganizationSettings,
        },
        queryKey: "settingsOrganization",
    } as const),
    senders: new RSEndpoint({
        path: "/settings/organizations/:orgName/senders",
        methods: {
            [HTTPMethods.GET]: {} as RSSender[],
        },
        queryKey: "servicesSenders",
    } as const),
    sender: new RSEndpoint({
        path: "/settings/organizations/:orgName/senders/:sender",
        methods: {
            [HTTPMethods.GET]: {} as RSSender,
            [HTTPMethods.POST]: {} as RSSender,
            [HTTPMethods.PUT]: {} as RSSender,
            [HTTPMethods.DELETE]: {} as RSSender,
        },
        queryKey: "servicesSenderDetail",
    } as const),
    receivers: new RSEndpoint({
        path: "/settings/organizations/:orgName/receivers",
        methods: {
            [HTTPMethods.GET]: {} as RSReceiver[],
        },
        queryKey: "servicesReceivers",
    } as const),
    receiver: new RSEndpoint({
        path: "/settings/organizations/:orgName/receivers/:receiverId",
        methods: {
            [HTTPMethods.GET]: {} as RSReceiver,
            [HTTPMethods.POST]: {} as RSReceiver,
            [HTTPMethods.PUT]: {} as RSReceiver,
            [HTTPMethods.DELETE]: {} as RSReceiver,
        },
        queryKey: "servicesReceiver",
    } as const),
};
