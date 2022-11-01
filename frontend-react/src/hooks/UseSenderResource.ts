import { useCallback } from "react";

import { useSessionContext } from "../contexts/SessionContext";
import { RSService, servicesEndpoints } from "../config/endpoints/services";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";

interface SenderKeys {
    scope: string;
    keys: {}[];
}

export interface RSSender extends RSService {
    allowDuplicates: boolean;
    createdAt?: string;
    createdBy?: string;
    customerStatus: string;
    format: string;
    keys?: SenderKeys;
    name: string;
    organizationName: string;
    primarySubmissionMethod?: string;
    processingType: string;
    schemaName: string;
    senderType?: string;
    topic: string;
    version?: number;
}

const { senderDetail } = servicesEndpoints;
export const useSenderResource = () => {
    const { authorizedFetch, rsUseQuery } = useAuthorizedFetch<RSService>();
    /* Access the session. */
    const { activeMembership } = useSessionContext();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(senderDetail, {
                segments: {
                    orgName: activeMembership?.parsedName!!,
                    sender: activeMembership?.service!!,
                },
            }),
        [
            activeMembership?.parsedName,
            activeMembership?.service,
            authorizedFetch,
        ]
    );
    const { data } = rsUseQuery(
        [senderDetail.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled:
                !!activeMembership?.parsedName && !!activeMembership.service,
        }
    );
    return { senderDetail: data };
};
