import { useCallback, useMemo } from "react";

import { watersEndpoints } from "../../config/api/waters";
import { ContentType } from "../UseFileHandler";

import { useRSMutation } from "./UseRSQuery";

export interface WatersPostArgs {
    client: string;
    fileName: string;
    fileContent: string;
    contentType?: ContentType;
}

const { upload, validate } = watersEndpoints;

// TODO: Return full mutation object
/** Uploads a file to ReportStream */
export const useWatersUploader = (
    uploaderCallback: (...args: any) => void,
    validateOnly: boolean = false
) => {
    /* Conditionally set the endpoint */
    const endpoint = useMemo(
        () => (validateOnly ? validate : upload),
        [validateOnly]
    );
    const mutationFunction = useCallback(
        ({ contentType, fileContent, client, fileName }: WatersPostArgs) => {
            return {
                headers: {
                    "Content-Type": contentType || ContentType.CSV,
                    payloadName: fileName,
                    client: client,
                },
                data: fileContent,
            };
        },
        []
    );

    const mutation = useRSMutation(endpoint, "POST", mutationFunction, {
        onSuccess: (data) => uploaderCallback(data),
        onError: (error) => uploaderCallback(error),
    });

    return {
        sendFile: mutation.mutateAsync,
        isWorking: mutation.isLoading,
        uploaderError: mutation.error,
    };
};
