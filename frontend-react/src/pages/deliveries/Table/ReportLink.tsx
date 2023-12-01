import download from "downloadjs";
import { Button, Icon } from "@trussworks/react-uswds";
import React from "react";

import config from "../../../config";
import { useSessionContext } from "../../../contexts/SessionContext";

const { RS_API_URL } = config;

interface ReportLinkProps {
    reportId: string;
    fileType?: string;

    /* OPTIONAL
    This boolean flag changes the return value from a standard <a> link to a <Button> (USWDS)
    so this single component can be used in Deliveries.tsx and DeliveryDetail.tsx */
    button?: boolean;
}

const formatFileType = (fileType: string) => {
    if (fileType === "HL7_BATCH") return "HL7";
    return fileType;
};

/*
    This element provides a download link on each row of the table and on the report
    details page
*/
function ReportLink({
    reportId,
    fileType,
    children,
    button,
}: React.PropsWithChildren<ReportLinkProps>) {
    const { authState } = useSessionContext();
    const { activeMembership } = useSessionContext();
    const organization = activeMembership?.parsedName;

    const handleClick = (e: any) => {
        e.preventDefault();
        if (reportId !== undefined) {
            fetch(`${RS_API_URL}/api/history/report/${reportId}`, {
                headers: {
                    authorization: `Bearer ${authState?.accessToken?.accessToken}`,
                    organization: organization!,
                },
            })
                .then((res) => res.json())
                .then((report) => {
                    // The filename to use for the download should not contain blob folders if present
                    let filename = decodeURIComponent(report.fileName);
                    let filenameStartIndex = filename.lastIndexOf("/");
                    if (
                        filenameStartIndex >= 0 &&
                        filename.length > filenameStartIndex + 1
                    )
                        filename = filename.substring(filenameStartIndex + 1);
                    download(report.content, filename, report.mimetype);
                })
                .catch((error) => console.error(error));
        }
    };

    if (!button) {
        return (
            <Button unstyled type="button" onClick={handleClick}>
                {fileType !== undefined ? formatFileType(fileType) : ""}
                {children}
            </Button>
        );
    } else {
        return (
            <>
                {fileType !== undefined && (
                    <Button
                        type="button"
                        outline
                        onClick={handleClick}
                        className="usa-button usa-button--outline float-right display-flex flex-align-center margin-left-1"
                    >
                        {formatFileType(fileType)}{" "}
                        <Icon.FileDownload className="margin-left-1" size={3} />
                    </Button>
                )}
            </>
        );
    }
}

export default ReportLink;
