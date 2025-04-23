import { useMutation } from "@tanstack/react-query";
import { useCallback, useState } from "react";
import { useParams } from "react-router";
import { reportsEndpoints, RSMessage, RSMessageResult } from "../../../../config/endpoints/reports";
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

    const [testMessage, setTestMessage] = useState<RSMessage | null>(null);
    const mutationFn = useCallback(
        async ({ testMessage }: RSMessage | null) => {
            return await authorizedFetch<RSMessageResult>(
                {
                    params: {
                        receiverName: receivername,
                        organizationName: orgname,
                        senderId: testMessage?.senderId,
                    },
                    data: testMessage?.reportBody,
                },
                testResult,
            );
        },
        [authorizedFetch, orgname, receivername, testMessage],
    );

    // Use 'enabled' to conditionally run the query whenever `requestBody` changes
    // and the user is an admin. If requestBody is empty or user isn't admin, no fetch is made.
    const mutation = useMutation<RSMessageResult, Error>({
        mutationFn,
    });
    // const mutation = useMutation<RSMessageResult, Error>({
    //     queryKey: [testResult.queryKey, activeMembership, receivername, testMessage],
    //     queryFn: fetchData,
    //     enabled: isAdmin && Boolean(testMessage),
    //     staleTime: 0,
    //     gcTime: 0,
    // });

    return {
        ...mutation,
        setTestMessage,
        isDisabled: !isAdmin,
    };
};

export default useTestMessageResult;
