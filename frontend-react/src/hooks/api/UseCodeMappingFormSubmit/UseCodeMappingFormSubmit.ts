import { useQuery } from "@tanstack/react-query";
import { useCallback, useState } from "react";
import { ConditionCodeData, conditionCodeEndpoints } from "../../../config/endpoints/conditionCode";
import useSessionContext from "../../../contexts/Session/useSessionContext";

const { mapSenderCode } = conditionCodeEndpoints;

const useCodeMappingFormSubmit = () => {
    const { activeMembership, authorizedFetch } = useSessionContext();
    const [requestBody, setRequestBody] = useState<File | null>(null);
    const [client, setClient] = useState("");

    const fetchData = useCallback(async () => {
        try {
            // Attempt the fetch
            console.log("requestBody = ", requestBody);
            const result = await authorizedFetch<ConditionCodeData[]>(
                {
                    data: requestBody,
                    headers: {
                        "Content-Type": "text/csv",
                        client: client,
                    },
                },
                mapSenderCode,
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
    }, [authorizedFetch, requestBody]);

    const useQueryResult = useQuery<ConditionCodeData[], Error>({
        queryKey: [mapSenderCode.queryKey, activeMembership, requestBody],
        queryFn: fetchData,
        enabled: Boolean(requestBody),
        staleTime: 0,
        gcTime: 0,
    });

    const { data } = useQueryResult;

    return {
        ...useQueryResult,
        data: data ?? [],
        setRequestBody,
        setClient,
    };
};

export default useCodeMappingFormSubmit;
