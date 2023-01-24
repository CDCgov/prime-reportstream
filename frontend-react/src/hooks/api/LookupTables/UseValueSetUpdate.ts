import {
    lookupTablesEndpoints,
    ValueSetRow,
} from "../../../config/api/lookupTables";
import { useRSMutation } from "../UseRSQuery";

interface UseValueSetUpdateMutationProps {
    data: ValueSetRow[];
    tableName: string;
}

// TODO: Return full mutation object
export const useValueSetUpdate = () => {
    const updateValueSet = ({
        data,
        tableName,
    }: UseValueSetUpdateMutationProps) => {
        return {
            segments: { tableName: tableName },
            data,
        };
    };

    const mutation = useRSMutation(
        lookupTablesEndpoints.updateLookupTable,
        "POST",
        updateValueSet
    );

    return {
        saveData: mutation.mutateAsync,
        isSaving: mutation.isLoading,
        saveError: mutation.error,
    };
};
