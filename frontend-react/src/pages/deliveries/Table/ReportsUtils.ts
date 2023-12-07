import download from "downloadjs";
import axios from "axios";

import { RSReportInterface } from "../../../network/api/History/Reports";
import config from "../../../config";

const { RS_API_URL } = config;
export const reportDetailURL = (id: string, base?: string) =>
    `${base ? base : RS_API_URL}/api/history/report/${id}`;

export const getReportAndDownload = (
    id: string,
    oktaToken: string,
    org: string,
): RSReportInterface | undefined => {
    let report = undefined;
    axios
        .get<RSReportInterface>(reportDetailURL(id), {
            headers: {
                Authorization: `Bearer ${oktaToken}`,
                Organization: org,
            },
        })
        .then((res) => res.data)
        .then((apiReport) => {
            // The filename to use for the download should not contain blob folders if present
            report = apiReport;
            downloadReport(apiReport);
        })
        .catch((error) => {
            console.error(error);
        });
    return report;
};

export const downloadReport = (report: RSReportInterface) => {
    let filename = decodeURIComponent(report.fileName);
    let filenameStartIndex = filename.lastIndexOf("/");
    if (filenameStartIndex >= 0 && filename.length > filenameStartIndex + 1)
        filename = filename.substring(filenameStartIndex + 1);
    return download(report.content, filename, report.mimeType);
};
