import axios from "axios";

import config from "../../../config";
import { RSReportInterface } from "../../../utils/ReportUtils";

const { RS_API_URL } = config;
export const reportDetailURL = (id: string, base?: string) => `${base ?? RS_API_URL}/api/history/report/${id}`;

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
    // Decode the file name from the report
    let filename = decodeURIComponent(report.fileName);

    // Extract the filename if it contains a path
    const filenameStartIndex = filename.lastIndexOf("/");
    if (filenameStartIndex >= 0 && filename.length > filenameStartIndex + 1) {
        filename = filename.substring(filenameStartIndex + 1);
    }

    const blob = new Blob([report.content], { type: report.mimeType || "application/octet-stream" });

    const url = URL.createObjectURL(blob);

    // Create a temporary anchor element to trigger the download
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;

    // Append the anchor to the DOM, trigger the download, and clean up
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
};
