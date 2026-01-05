import { useSuspenseQuery } from "@tanstack/react-query";

import useSessionContext from "../../../../contexts/Session/useSessionContext";

interface RSOrganizationSubmissionsSearchParams {
    organization: string;
    sortdir?: string;
    sortcol?: string;
    cursor?: string;
    since?: string;
    until?: string;
    pageSize?: number;
    showFailed?: boolean;
    [k: string]: string | number | boolean | undefined;
}

export interface RSOrganizationSubmission {
    submissionId: number;
    timestamp: string; // format is "2022-02-01T15:11:58.200754Z"
    sender: string;
    httpStatus: number;
    externalName: string;
    fileDisplayName: string;
    fileName: string;
    fileType: string;
    id: string | undefined;
    topic: string;
    reportItemCount: number;
    warningCount: number;
    errorCount: number;
}

export const FALLBACKDATE = "2020-01-01T00:00:00.000Z";

// TODO Implement in pages
function useOrganizationSubmissions(
    params: RSOrganizationSubmissionsSearchParams,
) {
    const { authorizedFetch } = useSessionContext();

    const fn = () => {
        return authorizedFetch<RSOrganizationSubmission[]>({
            url: `/waters/org/${params.organization}/submissions`,
            params,
        });
    };
    return useSuspenseQuery({
        queryKey: ["organizationSubmissions", params],
        queryFn: fn,
    });
}

export default useOrganizationSubmissions;
