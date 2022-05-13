import download from "downloadjs";
import axios from "axios";

import ReportResource from "../../../resources/ReportResource";

export const reportDetailURL = (id: string, base?: string) =>
    `${
        base ? base : process.env.REACT_APP_BACKEND_URL
    }/api/history/report/${id}`;

export const getReportAndDownload = (
    id: string,
    oktaToken: string,
    org: string
): ReportResource | undefined => {
    let report: ReportResource | undefined = undefined;
    axios
        .get<ReportResource>(reportDetailURL(id), {
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
            console.log(error);
        });
    return report;
};

export const downloadReport = (report: ReportResource) => {
    let filename = decodeURIComponent(report.fileName);
    let filenameStartIndex = filename.lastIndexOf("/");
    if (filenameStartIndex >= 0 && filename.length > filenameStartIndex + 1)
        filename = filename.substring(filenameStartIndex + 1);
    return download(report.content, filename, report.mimeType);
};
