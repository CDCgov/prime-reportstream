import { useMutation } from "@tanstack/react-query";

import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import {
    lookupTablesEndpoints,
    LookupTable,
} from "../config/endpoints/lookupTables";
import { RSNetworkError } from "../utils/RSNetworkError";

const { activateTable } = lookupTablesEndpoints;

interface ActivateValueSetOptions {
    tableVersion: number;
    tableName: string;
}

export const useValueSetActivation = () => {
    const { authorizedFetch } = useAuthorizedFetch<LookupTable>();
    const activateValueSet = ({
        tableVersion,
        tableName,
    }: ActivateValueSetOptions) => {
        return authorizedFetch(activateTable, {
            segments: {
                tableName,
                version: `${tableVersion}`,
            },
        });
    };
    const mutation = useMutation<
        LookupTable,
        RSNetworkError,
        ActivateValueSetOptions
    >(activateValueSet);
    return {
        activateTable: mutation.mutateAsync,
        isActivating: mutation.isLoading,
        activationError: mutation.error,
    };
};
