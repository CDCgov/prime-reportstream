import { useCallback } from "react";
import { useQuery } from "@tanstack/react-query";
import { createQuery } from "react-query-kit";

import {
    RSOrganizationSettings,
    servicesEndpoints,
} from "../config/endpoints/settings";
import { useAuthorizedFetch } from "../contexts/AuthorizedFetch";
import { useSessionContext } from "../contexts/Session";
import { getAuthFetchProps } from "../network/Middleware";

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

export function isOrganizationsMissingTransport(orgName: string): boolean {
    return ReceiverOrganizationsMissingTransport.indexOf(orgName) > -1;
}

export const useOrganizationSettings = () => {
    const { activeMembership } = useSessionContext();
    const parsedName = activeMembership?.parsedName;

    const authorizedFetch = useAuthorizedFetch<RSOrganizationSettings>();
    const memoizedDataFetch = useCallback(
        () =>
            authorizedFetch(settings, {
                segments: {
                    orgName: parsedName!!,
                },
            }),
        [parsedName, authorizedFetch],
    );
    return useQuery({
        queryKey: [settings.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
        enabled:
            Boolean(parsedName) && parsedName !== Organizations.PRIMEADMINS,
    });
};

const { authFetch, authMiddleware } =
    getAuthFetchProps<RSOrganizationSettings>();

/**
 * Experimental replacement hook using middleware for controlling enablement
 * of the hook and variables. Will be iterated on to determine best ABI.
 */
export const useOrganizationSettings__ = createQuery({
    primaryKey: settings.queryKey,
    queryFn: authFetch,
    use: [
        (useQueryNext) => (options) => {
            const { activeMembership } = useSessionContext();
            const newOptions = {
                ...options,
                variables: {
                    ...options.variables,
                    endpoint: settings,
                    fetchConfig: {
                        ...options.variables?.fetchConfig,
                        segments: {
                            orgName: activeMembership?.parsedName,
                        },
                    },
                },
                enabled:
                    (options.enabled == null || options.enabled) &&
                    Boolean(activeMembership?.parsedName) &&
                    activeMembership?.parsedName !== Organizations.PRIMEADMINS,
            };

            return useQueryNext(newOptions);
        },
        authMiddleware,
    ],
});
