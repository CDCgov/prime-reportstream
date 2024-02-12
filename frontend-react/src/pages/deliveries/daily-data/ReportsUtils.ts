import axios from "axios";
import download from "downloadjs";

import config from "../../../config";
import { RSReportInterface } from "../../../network/api/History/Reports";

const { RS_API_URL } = config;
export const reportDetailURL = (id: string, base?: string) =>
    `${base ? base : RS_API_URL}/api/history/report/${id}`;

export const getReportAndDownload = (
    reportId: string,
    oktaToken: string,
    org: string,
): RSReportInterface | undefined => {
    let report = undefined;
    void axios
        .get<RSReportInterface>(reportDetailURL(reportId), {
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
        });
    return report;
};

export const downloadReport = (report: RSReportInterface) => {
    let filename = decodeURIComponent(report.fileName);
    const filenameStartIndex = filename.lastIndexOf("/");
    if (filenameStartIndex >= 0 && filename.length > filenameStartIndex + 1)
        filename = filename.substring(filenameStartIndex + 1);
    return download(report.content, filename, report.mimeType);
};
