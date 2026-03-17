import { Button, Icon } from "@trussworks/react-uswds";
import { PropsWithChildren } from "react";

import { downloadReport } from "./ReportsUtils";
import config from "../../../config";
import useSessionContext from "../../../contexts/Session/useSessionContext";
import { isDateExpired } from "../../../utils/DateTimeUtils";

const { RS_API_URL } = config;

interface ReportLinkProps {
    reportId: string;
    reportExpires?: string | number;
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
function ReportLink({ reportId, fileType, reportExpires, children, button }: PropsWithChildren<ReportLinkProps>) {
    const { authState } = useSessionContext();
    const { activeMembership } = useSessionContext();
    const organization = activeMembership?.parsedName;

    const handleClick = (e: any) => {
        e.preventDefault();
        if (reportId !== undefined) {
            void fetch(`${RS_API_URL}/api/history/report/${reportId}`, {
                headers: {
                    authorization: `Bearer ${authState?.accessToken?.accessToken}`,
                    organization: organization!,
                },
            })
                .then((res) => res.json())
                .then((report) => {
                    downloadReport(report);
                });
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
                {fileType !== undefined && !isDateExpired(reportExpires ?? "") && (
                    <Button
                        type="button"
                        outline
                        onClick={handleClick}
                        className="usa-button usa-button--outline float-right display-flex flex-align-center margin-left-1"
                    >
                        {formatFileType(fileType)} <Icon.FileDownload className="margin-left-1" size={3} />
                    </Button>
                )}
            </>
        );
    }
}

export default ReportLink;
