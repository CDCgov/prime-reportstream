import React, { useEffect } from "react";

import {
    formattedDateFromTimestamp,
    timeZoneAbbreviated,
} from "../../utils/DateTimeUtils";
import { StaticAlert } from "../StaticAlert";
import { FileResponseError } from "../../network/api/WatersApi";

type ExtendedSuccessMetadata = {
    destinations?: string;
    reportId?: string;
    timestamp?: string;
};

type FileSuccessDisplayProps = {
    fileName: string;
    heading: string;
    message: string;
    showExtendedMetadata: boolean;
    extendedMetadata?: ExtendedSuccessMetadata;
};

export const FileSuccessDisplay = ({
    fileName,
    heading,
    message,
    showExtendedMetadata,
    extendedMetadata = {},
}: FileSuccessDisplayProps) => {
    const { destinations, timestamp, reportId } = extendedMetadata;
    const destinationsDisplay =
        destinations || "There are no known recipients at this time.";

    return (
        <>
            <StaticAlert type={"success"} heading={heading} message={message} />
            <div>
                <p
                    id="validatedFilename"
                    className="text-normal text-base margin-bottom-0"
                >
                    File name
                </p>
                <p className="margin-top-05">{fileName}</p>
                {showExtendedMetadata && (
                    <>
                        {reportId && (
                            <div>
                                <p className="text-normal text-base margin-bottom-0">
                                    Confirmation Code
                                </p>
                                <p className="margin-top-05">{reportId}</p>
                            </div>
                        )}
                        {timestamp && (
                            <div>
                                <p className="text-normal text-base margin-bottom-0">
                                    Date Received
                                </p>
                                <p className="margin-top-05">
                                    {formattedDateFromTimestamp(
                                        timestamp,
                                        "DD MMMM YYYY"
                                    )}
                                </p>
                            </div>
                        )}
                        {timestamp && (
                            <div>
                                <p className="text-normal text-base margin-bottom-0">
                                    Time Received
                                </p>
                                <p className="margin-top-05">{`${formattedDateFromTimestamp(
                                    timestamp,
                                    "h:mm"
                                )} ${timeZoneAbbreviated()}`}</p>
                            </div>
                        )}
                        <div>
                            <p className="text-normal text-base margin-bottom-0">
                                Recipients
                            </p>
                            <p className="margin-top-05">
                                {destinationsDisplay}
                            </p>
                        </div>
                    </>
                )}
            </div>
        </>
    );
};

/***
 * This function attempts to truncate an error message if it contains
 * a full stack trace
 * @param errorMessage - the error message to potentially reformat
 * @returns - the original or transformed error message
 */
const truncateErrorMesssage = (errorMessage: string | undefined): string => {
    if (!errorMessage) return "";

    if (errorMessage.includes("\n") && errorMessage.includes("Exception:"))
        return errorMessage.substring(0, errorMessage.indexOf("\n")) + " ...";

    return errorMessage;
};

type FileErrorDisplayProps = {
    errors: FileResponseError[];
    message: string;
    fileName: string;
    handlerType: string;
    heading: string;
};

export const FileErrorDisplay = ({
    fileName,
    errors,
    message,
    heading,
    handlerType,
}: FileErrorDisplayProps) => {
    const showErrorTable =
        errors && errors.length && errors.some((error) => error.message);

    useEffect(() => {
        errors.forEach((error: FileResponseError) => {
            if (error.details) {
                console.error(`${handlerType} failure: ${error.details}`);
            }
        });
    }, [errors, handlerType]);

    return (
        <div>
            <StaticAlert type={"error"} heading={heading} message={message} />
            <div>
                <p
                    id="validatedFilename"
                    className="text-normal text-base margin-bottom-0"
                >
                    File name
                </p>
                <p className="margin-top-05">{fileName}</p>
            </div>
            {showErrorTable && (
                <table className="usa-table usa-table--borderless">
                    <thead>
                        <tr>
                            <th>Requested Edit</th>
                            <th>Areas Containing the Requested Edit</th>
                        </tr>
                    </thead>
                    <tbody>
                        {errors.map((e, i) => {
                            return (
                                <tr key={"error_" + i}>
                                    <td>{truncateErrorMesssage(e.message)}</td>
                                    <td>
                                        {e.rowList && (
                                            <span>Row(s): {e.rowList}</span>
                                        )}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            )}
        </div>
    );
};

interface FileWarningBannerProps {
    message: string;
}

export const FileWarningBanner = ({ message }: FileWarningBannerProps) => {
    return <StaticAlert type={"warning"} heading="Warning" message={message} />;
};

type FileWarningsDisplayProps = {
    warnings: FileResponseError[];
    message: string;
    // fileName: string;
    // handlerType: string;
    heading: string;
};

export const FileWarningsDisplay = ({
    // fileName,
    warnings,
    message,
    heading,
}: // handlerType,
FileWarningsDisplayProps) => {
    return (
        <div>
            <StaticAlert type={"warning"} heading={heading} message={message} />
            <h3>Warnings</h3>
            <table className="usa-table usa-table--borderless">
                <thead>
                    <tr>
                        <th>Warning</th>
                        <th>Areas Containing the Requested Edit</th>
                    </tr>
                </thead>
                <tbody>
                    {warnings.map((w, i) => {
                        return (
                            <tr key={"error_" + i}>
                                <td>{w.message}</td>
                                <td>
                                    {w.rowList && (
                                        <span>Row(s): {w.rowList}</span>
                                    )}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
};
