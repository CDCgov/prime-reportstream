import { useMutation } from "@tanstack/react-query";
import { useCallback, useState } from "react";
import { ConditionCodeData, conditionCodeEndpoints } from "../../../config/endpoints/conditionCode";
import useSessionContext from "../../../contexts/Session/useSessionContext";

const { mapSenderCode } = conditionCodeEndpoints;

interface CodeMappingVariables {
    file: string;
    client: string;
}

function useCodeMappingFormSubmit() {
    const { authorizedFetch } = useSessionContext();
    const [client, setClient] = useState("");

    const mutationFn = useCallback(
        async ({ file, client }: CodeMappingVariables) => {
            return authorizedFetch<ConditionCodeData[]>(
                {
                    data: file,
                    headers: {
                        "Content-Type": "text/csv",
                        client,
                    },
                },
                mapSenderCode,
            );
        },
        [authorizedFetch],
    );

    const mutation = useMutation<ConditionCodeData[], Error, CodeMappingVariables>({
        mutationFn,
    });

    return {
        ...mutation,
        client,
        setClient,
    };
}

export default useCodeMappingFormSubmit;
