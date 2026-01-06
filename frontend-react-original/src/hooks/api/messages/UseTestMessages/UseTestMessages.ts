import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";
import { reportsEndpoints, RSMessage } from "../../../../config/endpoints/reports";
import useSessionContext from "../../../../contexts/Session/useSessionContext";
import { Organizations } from "../../../UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";

const { test } = reportsEndpoints;

/**
 * Custom hook to fetch and manage "Test Messages" data for the current session.
 *
 * @description
 * This hook fetches "Test Messages" from the backend. While the UI and design refer to this feature as
 * "Test Messages," the corresponding API endpoint is `/api/reports/testing`, which uses the "Reports" nomenclature.
 * This discrepancy exists between the backend naming convention ("Reports") and the frontend display ("Messages").
 *
 * @returns {object} The hook returns the following:
 * - `data` (`RSMessage[] | undefined`): The fetched array of test messages.
 * - Other properties from `useSuspenseQuery` (e.g., `isLoading`, `isError`, `error`).
 */

const useTestMessages = () => {
    const { activeMembership, authorizedFetch } = useSessionContext();
    const parsedName = activeMembership?.parsedName;
    const isAdmin = Boolean(parsedName) && parsedName === Organizations.PRIMEADMINS;

    const memoizedDataFetch = useCallback(() => {
        if (isAdmin) {
            return authorizedFetch<RSMessage[]>({}, test);
        }
        return null;
    }, [isAdmin, authorizedFetch]);
    const useSuspenseQueryResult = useSuspenseQuery({
        queryKey: [test.queryKey, activeMembership],
        queryFn: memoizedDataFetch,
    });

    const { data } = useSuspenseQueryResult;

    return {
        ...useSuspenseQueryResult,
        data: data ?? [],
    };
};

export default useTestMessages;
