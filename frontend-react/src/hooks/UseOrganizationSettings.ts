import { useCallback } from "react";

import {
    RSOrganizationSettings,
    servicesEndpoints,
} from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import { useSessionContext } from "../contexts/SessionContext";

import { Organizations } from "./UseAdminSafeOrganizationName";

const { settings } = servicesEndpoints;

/*
    orOrganizations that are not connected to the API to receive reports through the SEND step.
    A receiver that has no transport configuration
 */
export const ReceiverOrganizationsMissingTransport: string[] = [
    "ca-sbc-phd",
    "ca-scc-phd",
    "co-routt-county-phd",
    "co-san-juan-basin-phd",
    "fl-hillsborough-phd",
    "oh-ccchd-doh",
    "pa-chester-phd",
    "pa-montgomery-phd",
    "pa-philadelphia-phd",
    "pima-az-phd",
];

export const useOrganizationSettings = () => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const { authorizedFetch, rsUseQuery } =
        useAuthorizedFetch<RSOrganizationSettings>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(settings, {
                segments: {
                    orgName: parsedName!!,
                },
            }),
        [parsedName, authorizedFetch],
    );
    return rsUseQuery(
        [settings.queryKey, activeMembership],
        memoizedDataFetch,
        {
            enabled:
                Boolean(parsedName) && parsedName !== Organizations.PRIMEADMINS,
        },
    );
};
