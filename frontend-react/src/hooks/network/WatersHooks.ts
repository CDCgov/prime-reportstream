import { useMutation } from "@tanstack/react-query";
import { useMemo } from "react";

import { useAuthorizedFetch } from "../../contexts/AuthorizedFetchContext";
import { watersEndpoints, WatersResponse } from "../../network/api/WatersApi";
import { RSNetworkError } from "../../utils/RSNetworkError";
import { ContentType } from "../UseFileHandler";

export interface WatersPostArgs {
    client: string;
    fileName: string;
    fileContent: string;
    contentType?: ContentType;
}

const { upload, validate } = watersEndpoints;

/** Uploads a file to ReportStream */
export const useWatersUploader = (validateOnly: boolean = false) => {
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
    >(mutationFunction);
    return {
        sendFile: mutation.mutateAsync,
        isWorking: mutation.isLoading,
    };
};
