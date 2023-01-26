import { HTTPMethods, RSEndpoint } from "./RSEndpoint";

/*
Waters Endpoints
* waters -> uploads a file to the ReportStream service
* validate -> validates a file against ReportStream file requirements (filters, data quality, etc.)
*/
export const watersEndpoints = {
    upload: new RSEndpoint({
        path: "/waters",
        methods: {
            [HTTPMethods.POST]: {} as WatersResponse,
        },
        queryKey: "watersPost",
    } as const),
    validate: new RSEndpoint({
        path: "/validate",
        methods: {
            [HTTPMethods.POST]: {} as WatersResponse,
        },
        queryKey: "watersValidate",
    } as const),
};
