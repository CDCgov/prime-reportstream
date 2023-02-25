import { useMutation } from "@tanstack/react-query";

import { useAuthorizedFetch } from "../contexts/AuthorizedFetchContext";
import {
    lookupTablesEndpoints,
    LookupTable,
    ValueSetRow,
} from "../config/endpoints/lookupTables";
import { RSNetworkError } from "../utils/RSNetworkError";

const { updateTable } = lookupTablesEndpoints;

interface UpdateValueSetOptions {
    data: ValueSetRow[];
    tableName: string;
}

export const useValueSetUpdate = () => {
    const { authorizedFetch } = useAuthorizedFetch<LookupTable>();

    const updateValueSet = ({ data, tableName }: UpdateValueSetOptions) => {
        return authorizedFetch(updateTable, {
            segments: { tableName: tableName },
            data,
        });
    };

    // generic signature is defined here https://github.com/TanStack/query/blob/4690b585722d2b71d9b87a81cb139062d3e05c9c/packages/react-query/src/useMutation.ts#L66
    // <type of data returned, type of error returned, type of variables passed to mutate fn, type of context (?)>
    const mutation = useMutation<
        LookupTable,
        RSNetworkError,
        UpdateValueSetOptions
    >(updateValueSet);
    return {
        saveData: mutation.mutateAsync,
        isSaving: mutation.isLoading,
        saveError: mutation.error,
    };
};
