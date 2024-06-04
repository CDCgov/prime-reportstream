import { useMutation } from "@tanstack/react-query";
import download from "downloadjs";
import useSessionContext from "../../../contexts/Session/useSessionContext";
import { RSReportHistory } from "../UseReportHistory/UseReportHistory";

export interface RSReportHistorySearchParams {
    id: string;
}

/**
 * Mutate function to initiate download of a report's file (ex: initiate in
 * event handlers).
 */
const useReportHistoryFileDownload = ({
    id,
    ...params
}: RSReportHistorySearchParams) => {
    const { authorizedFetch } = useSessionContext();

    const fn = async () => {
        if (!id) throw new Error("Report id missing");

        const { fileName, mimeType, content } =
            await authorizedFetch<RSReportHistory>({
                url: `/history/report${id}`,
                params,
            });

        // The filename to use for the download should not contain blob folders if present
        let filename = decodeURIComponent(fileName);
        const filenameStartIndex = filename.lastIndexOf("/");
        if (filenameStartIndex >= 0 && filename.length > filenameStartIndex + 1)
            filename = filename.substring(filenameStartIndex + 1);
        download(content, filename, mimeType);
    };

    return useMutation({
        mutationFn: fn,
    });
};

export default useReportHistoryFileDownload;
