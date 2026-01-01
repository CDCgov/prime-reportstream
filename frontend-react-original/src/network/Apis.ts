import { AxiosRequestHeaders } from "axios";

// this is duplicative with the Api class, but in order to split the functionality in this file
// out to enable testing, we need a definition of Api that can function outside of the class declaration
// todo: improve this state of affairs
interface ReportStreamApi {
    basePath: string;
    headers: AxiosRequestHeaders;
    root: string;
    updateSession: (headers: ReportStreamApiHeaders) => void;
}

const apis: ReportStreamApi[] = [];

interface ReportStreamApiHeaders {
    Authorization: string;
    Organization: string;
}

// update auth / session info for all registered APIs using the Api.ts fetch method
// to be run whenever auth or session information is updated in the application
export const updateApiSessions = ({
    token,
    organization,
}: {
    token: string;
    organization: string;
}) => {
    const headers = {
        Authorization: `Bearer ${token || ""}`,
        Organization: organization || "",
    };
    apis.forEach((api) => api.updateSession(headers));
};

export const registerApi = (api: ReportStreamApi) => {
    apis.push(api);
};
