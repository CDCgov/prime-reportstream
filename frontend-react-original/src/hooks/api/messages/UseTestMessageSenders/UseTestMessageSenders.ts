import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";
import { reportsEndpoints, RSMessageSender } from "../../../../config/endpoints/reports";
import useSessionContext from "../../../../contexts/Session/useSessionContext";
import { Organizations } from "../../../UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";

const { testSenders } = reportsEndpoints;

/**
 * Custom hook to fetch "Test Message Sender" data for the current session.
 *
 * @description
 * An array of all the possible senders.
 *
 * @returns {object} The hook returns the following:
 * - `data` (`RSMessageSender[]`): The fetched array of possible senders.
 * - will always return an array, with at least a "None" option
 * - Other properties from `useSuspenseQuery` (e.g., `isLoading`, `isError`, `error`).
 */

const useTestMessageSenders = () => {
    const { activeMembership, authorizedFetch } = useSessionContext();
    const parsedName = activeMembership?.parsedName;
    const isAdmin = Boolean(parsedName) && parsedName === Organizations.PRIMEADMINS;

    const memoizedDataFetch = useCallback(() => {
        if (isAdmin) {
            return authorizedFetch<RSMessageSender[]>({}, testSenders);
        }
        return null;
    }, [isAdmin, authorizedFetch]);
    const useSuspenseQueryResult = useSuspenseQuery({
        queryKey: [testSenders.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
    });

    const { data } = useSuspenseQueryResult;

    return {
        ...useSuspenseQueryResult,
        data: data ?? [],
    };
};

export default useTestMessageSenders;
