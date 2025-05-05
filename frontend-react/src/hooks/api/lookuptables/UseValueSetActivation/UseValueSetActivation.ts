import { useMutation } from "@tanstack/react-query";

import { LookupTable, lookupTablesEndpoints } from "../../../../config/endpoints/lookupTables";
import useSessionContext from "../../../../contexts/Session/useSessionContext";
import { RSNetworkError } from "../../../../utils/RSNetworkError";

const { activateTable } = lookupTablesEndpoints;

export interface ActivateValueSetOptions {
    tableVersion: number;
    tableName: string;
}

export type UseValueSetActivationResult = ReturnType<typeof useValueSetActivation>;

const useValueSetActivation = () => {
    const { authorizedFetch } = useSessionContext();
    const activateValueSet = ({ tableVersion, tableName }: ActivateValueSetOptions) => {
        return authorizedFetch<LookupTable>(
            {
                segments: {
                    tableName,
                    version: `${tableVersion}`,
                },
            },
            activateTable,
        );
    };
    return useMutation<LookupTable, RSNetworkError, ActivateValueSetOptions>({
        mutationFn: activateValueSet,
    });
};

export default useValueSetActivation;
