import { useQuery } from "@tanstack/react-query";
import { useCallback, useState } from "react";
import { useParams } from "react-router";
import { reportsEndpoints, RSMessageResult } from "../../../../config/endpoints/reports";
import useSessionContext from "../../../../contexts/Session/useSessionContext";
import { Organizations } from "../../../UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";

const { testResult } = reportsEndpoints;

/**
 * Custom hook to fetch validation for a single test report.
 *
 * @returns {object} The hook returns the following:
 * @property {RSMessageResult | []} data - The fetched test message results (empty array if none).
 * @property {function} setRequestBody - A setter for the request body used by the query.
 * @property {boolean} isLoading - `true` while the query is fetching.
 * @property {boolean} isError - `true` if the query encountered an error.
 * @property {Error | null} error - The error object if any error occurred.
 * @property {"loading" | "error" | "success" | "idle"} status - The status of the query.
 * @property {boolean} isDisabled - Indicates whether the feature is disabled (for non-admin users).
 */
const useTestMessageResult = () => {
    const { activeMembership, authorizedFetch } = useSessionContext();
    const { orgname, receivername } = useParams();
    const parsedName = activeMembership?.parsedName;
    const isAdmin = Boolean(parsedName) && parsedName === Organizations.PRIMEADMINS;

    const [requestBody, setRequestBody] = useState<string | null>(null);

    const fetchData = useCallback(async () => {
        try {
            // Attempt the fetch
            const result = await authorizedFetch<RSMessageResult>(
                {
                    params: {
                        receiverName: receivername,
                        organizationName: orgname,
                    },
                    data: requestBody,
                },
                testResult,
            );

            return result;
        } catch (err) {
            // Ensure we're rejecting with an actual Error object
            if (err instanceof Error) {
                return Promise.reject(err);
            } else {
                return Promise.reject(new Error(String(err)));
            }
        }
    }, [authorizedFetch, orgname, receivername, requestBody]);

    // Use 'enabled' to conditionally run the query whenever `requestBody` changes
    // and the user is an admin. If requestBody is empty or user isn't admin, no fetch is made.
    const useQueryResult = useQuery<RSMessageResult, Error>({
        queryKey: [testResult.queryKey, activeMembership, receivername, requestBody],
        queryFn: fetchData,
        enabled: isAdmin && Boolean(requestBody),
        staleTime: 0,
        gcTime: 0,
    });

    const { data } = useQueryResult;

    return {
        ...useQueryResult,
        data: data ?? [],
        setRequestBody,
        isDisabled: !isAdmin,
    };
};

export default useTestMessageResult;
