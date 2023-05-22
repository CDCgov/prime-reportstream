import download from "downloadjs";
import { Button, Icon } from "@trussworks/react-uswds";
import { useOktaAuth } from "@okta/okta-react";
import React from "react";

import ReportResource from "../../../resources/ReportResource";
import { getStoredOrg } from "../../../utils/SessionStorageTools";
import config from "../../../config";
import { RSDelivery } from "../../../config/endpoints/deliveries";
import { isDateExpired } from "../../../utils/DateTimeUtils";

const { RS_API_URL } = config;

interface Props {
    /* REQUIRED
    A ReportResource is passed in using this property. This is necessary for download()
    since that function relies on the content, fileName, and mimeType properties */
    report: ReportResource | RSDelivery | undefined;

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
function ReportLink(props: Props) {
    const { authState } = useOktaAuth();
    const organization = getStoredOrg();

    const handleClick = (e: any) => {
        e.preventDefault();
        if (props.report !== undefined && props.report.reportId !== undefined) {
            let reportId = props.report.reportId;
            fetch(`${RS_API_URL}/api/history/report/${reportId}`, {
                headers: {
                    Authorization: `Bearer ${authState?.accessToken?.accessToken}`,
                    Organization: organization!,
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

    if (!props.button) {
        return (
            <Button unstyled type="button" onClick={handleClick}>
                {props.report !== undefined
                    ? formatFileType(props.report!.fileType)
                    : ""}
            </Button>
        );
    } else {
        return (
            <>
                {props.report !== undefined &&
                    !isDateExpired(props.report!.expires) && (
                        <Button
                            type="button"
                            outline
                            onClick={handleClick}
                            className="usa-button usa-button--outline float-right display-flex flex-align-center margin-left-1"
                        >
                            {formatFileType(props.report!.fileType)}{" "}
                            <Icon.FileDownload
                                className="margin-left-1"
                                size={3}
                            />
                        </Button>
                    )}
            </>
        );
    }
}

export default ReportLink;
