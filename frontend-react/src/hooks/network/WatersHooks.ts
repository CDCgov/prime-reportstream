import { useMutation } from "@tanstack/react-query";

import { useAuthorizedFetch } from "../../contexts/AuthorizedFetchContext";
import { watersEndpoints, WatersResponse } from "../../network/api/WatersApi";

export interface WatersValidateArgs {
    client: string;
    fileName: string;
    contentType: string;
    fileContent: string;
}

const { postValidate } = watersEndpoints;

export const useFileValidation = () => {
    const { authorizedFetch } = useAuthorizedFetch<WatersResponse>();
    const mutationFunction = (args: WatersValidateArgs) => {
        const { contentType, fileContent, client, fileName } = args;
        return authorizedFetch(postValidate, {
            headers: {
                "Content-Type": contentType,
                payloadName: fileName,
                client: client,
            },
            data: fileContent,
        });
    };
    const mutation = useMutation(mutationFunction);
    return {
        validateFile: mutation.mutateAsync,
        isValidating: mutation.isLoading,
    };
};
