import classNames from "classnames";
import { PropsWithChildren } from "react";

import useReportHistoryFileDownload from "../../hooks/api/UseReportHistoryFileDownload/UseReportHistoryFileDownload";
import { isDateExpired } from "../../utils/DateTimeUtils";
import { Button, ButtonProps } from "../Button/Button";
import Icon from "../Icon/Icon";

interface ReportFileDownloadButtonProps extends Omit<ButtonProps, "type"> {
    reportId: string;
    reportExpires?: string | number;
    fileType?: string;
    type?: ButtonProps["type"];
}

const formatFileType = (fileType: string) => {
    if (fileType === "HL7_BATCH") return "HL7";
    return fileType;
};

function ReportFileDownloadButton({
    reportId,
    fileType,
    reportExpires,
    children,
    unstyled,
    className,
    ...props
}: PropsWithChildren<ReportFileDownloadButtonProps>) {
    const { mutateAsync } = useReportHistoryFileDownload({ id: reportId });

    const handleClick = (e: any) => {
        e.preventDefault();
        if (reportId) {
            void mutateAsync();
        }
    };

    if (unstyled) {
        return (
            <Button unstyled type="button" onClick={handleClick} {...props}>
                {fileType !== undefined ? formatFileType(fileType) : ""}
                {children}
            </Button>
        );
    } else {
        const classes = classNames(
            "usa-button usa-button--outline float-right display-flex flex-align-center margin-left-1",
            className,
        );
        return (
            <>
                {fileType !== undefined &&
                    !isDateExpired(reportExpires ?? "") && (
                        <Button
                            type="button"
                            outline
                            onClick={handleClick}
                            className={classes}
                            icon={
                                <Icon
                                    name="FileDownload"
                                    className="margin-left-1"
                                    size={3}
                                />
                            }
                            {...props}
                        >
                            {formatFileType(fileType)}
                        </Button>
                    )}
            </>
        );
    }
}

export default ReportFileDownloadButton;
