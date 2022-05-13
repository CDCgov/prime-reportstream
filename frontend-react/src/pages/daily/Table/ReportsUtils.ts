import download from "downloadjs";

import ReportResource from "../../../resources/ReportResource";

export const reportDetailURL = (id: string, base?: string) =>
    `${
        base ? base : process.env.REACT_APP_BACKEND_URL
    }/api/history/report/${id}`;

export const getReport = (id: string, oktaToken: string, org: string) => {
    fetch(reportDetailURL(id), {
        headers: {
            Authorization: `Bearer ${oktaToken}`,
            Organization: org,
        },
    })
        .then((res) => res.json())
        .then((report) => {
            // The filename to use for the download should not contain blob folders if present
            downloadReport(report);
        })
        .catch((error) => console.log(error));
};

export const downloadReport = (report: ReportResource) => {
    let filename = decodeURIComponent(report.fileName);
    let filenameStartIndex = filename.lastIndexOf("/");
    if (filenameStartIndex >= 0 && filename.length > filenameStartIndex + 1)
        filename = filename.substring(filenameStartIndex + 1);
    download(report.content, filename, report.mimeType);
};
