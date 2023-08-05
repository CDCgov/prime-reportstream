import { UseMutateAsyncFunction, useMutation } from "@tanstack/react-query";

import { useAuthorizedFetch } from "../../contexts/AuthorizedFetchContext";
import { watersEndpoints, WatersResponse } from "../../config/endpoints/waters";
import { RSNetworkError } from "../../utils/RSNetworkError";
import { ContentType, FileType } from "../../utils/TemporarySettingsAPITypes";

export interface WatersPostArgs {
    client: string;
    fileName: string;
    fileContent: string;
    contentType?: ContentType;
    schema: string;
    format: FileType;
}

const { validate } = watersEndpoints;

export type UseWatersUploaderSendFileMutation = UseMutateAsyncFunction<
    WatersResponse,
    RSNetworkError<WatersResponse>,
    WatersPostArgs
>;

export type UseWatersUploaderResult = {
    sendFile: UseWatersUploaderSendFileMutation;
    isWorking: boolean;
    uploaderError: RSNetworkError<WatersResponse> | null;
};

export const FORMAT_TO_CONTENT_TYPE = {
    [FileType.CSV]: ContentType.CSV,
    [FileType.HL7]: ContentType.HL7,
};

/** Uploads a file to ReportStream */
export const useWatersUploader = (
    callback?: (data?: WatersResponse) => void,
) => {
    const { authorizedFetch } = useAuthorizedFetch<WatersResponse>();

    const mutationFunction = ({
        fileContent,
        client,
        fileName,
        schema,
        format,
    }: WatersPostArgs) => {
        return authorizedFetch(validate, {
            headers: {
                "Content-Type": FORMAT_TO_CONTENT_TYPE[format],
                payloadName: fileName,
                client: client,
            },
            data: fileContent,
            params: {
                format,
                schema,
            },
        });
    };
    const mutation = useMutation<
        WatersResponse,
        RSNetworkError<WatersResponse>,
        WatersPostArgs
    >(mutationFunction, {
        onSuccess: (data) => callback?.(data),
        // pass response data we stored in RSNetworkError on throw
        onError: (error) => callback?.(error.data),
    });
    return {
        sendFile: mutation.mutateAsync,
        isWorking: mutation.isLoading,
        uploaderError: mutation.error,
    };
};
