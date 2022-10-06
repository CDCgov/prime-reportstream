import { useMutation } from "@tanstack/react-query";
import { useMemo } from "react";

import { useAuthorizedFetch } from "../../contexts/AuthorizedFetchContext";
import { watersEndpoints, WatersResponse } from "../../config/endpoints/waters";
import { ContentType } from "../UseFileHandler";
import { RSNetworkError } from "../../utils/RSNetworkError";

export interface WatersPostArgs {
    client: string;
    fileName: string;
    fileContent: string;
    contentType?: ContentType;
}

const { upload, validate } = watersEndpoints;

/** Uploads a file to ReportStream */
export const useWatersUploader = (
    callback: (data: WatersResponse | undefined) => any,
    validateOnly: boolean = false
) => {
    const { authorizedFetch } = useAuthorizedFetch<WatersResponse>();
    /* Conditionally set the endpoint */
    const memoizedEndpoint = useMemo(
        () => (validateOnly ? validate : upload),
        [validateOnly]
    );
    const mutationFunction = ({
        contentType,
        fileContent,
        client,
        fileName,
    }: WatersPostArgs) => {
        return authorizedFetch(memoizedEndpoint, {
            headers: {
                "Content-Type": contentType || ContentType.CSV,
                payloadName: fileName,
                client: client,
            },
            data: fileContent,
        });
    };
    const mutation = useMutation<
        WatersResponse,
        RSNetworkError,
        WatersPostArgs
    >(mutationFunction, {
        onSuccess: (data) => callback(data),
        // pass response data we stored in RSNetworkError on throw
        onError: (error) => callback(error.data),
    });
    return {
        sendFile: mutation.mutateAsync,
        isWorking: mutation.isLoading,
        uploaderError: mutation.error,
    };
};
