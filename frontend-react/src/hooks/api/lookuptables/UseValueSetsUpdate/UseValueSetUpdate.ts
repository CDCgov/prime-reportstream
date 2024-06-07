import { useMutation } from "@tanstack/react-query";

import {
    LookupTable,
    lookupTablesEndpoints,
    ValueSetRow,
} from "../../../../config/endpoints/lookupTables";
import useSessionContext from "../../../../contexts/Session/useSessionContext";
import { RSNetworkError } from "../../../../utils/RSNetworkError";

const { updateTable } = lookupTablesEndpoints;

export interface UpdateValueSetOptions {
    data: ValueSetRow[];
    tableName: string;
}

export type UseValueSetUpdateResult = ReturnType<typeof useValueSetUpdate>;

const useValueSetUpdate = () => {
    const { authorizedFetch } = useSessionContext();

    const updateValueSet = ({ data, tableName }: UpdateValueSetOptions) => {
        return authorizedFetch<LookupTable>(
            {
                segments: { tableName: tableName },
                data,
            },
            updateTable,
        );
    };

    // generic signature is defined here https://github.com/TanStack/query/blob/4690b585722d2b71d9b87a81cb139062d3e05c9c/packages/react-query/src/useMutation.ts#L66
    // <type of data returned, type of error returned, type of variables passed to mutate fn, type of context (?)>
    return useMutation<LookupTable, RSNetworkError, UpdateValueSetOptions>({
        mutationFn: updateValueSet,
    });
};

export default useValueSetUpdate;
