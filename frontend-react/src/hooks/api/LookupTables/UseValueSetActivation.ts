import { useMutation } from "@tanstack/react-query";

import {
    lookupTablesEndpoints,
    LookupTable,
} from "../../../config/api/lookupTables";
import { RSNetworkError } from "../../../utils/RSNetworkError";

interface ActivateValueSetOptions {
    tableVersion: number;
    tableName: string;
}

export const useValueSetActivation = () => {
    const activateValueSet = ({
        tableVersion,
        tableName,
    }: ActivateValueSetOptions) => {
        return lookupTablesEndpoints.activateLookupTableVersion.fetchers.put({
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
    >(
        [lookupTablesEndpoints.activateLookupTableVersion.meta.queryKey],
        activateValueSet
    );
    return {
        activateTable: mutation.mutateAsync,
        isActivating: mutation.isLoading,
        activationError: mutation.error,
    };
};
