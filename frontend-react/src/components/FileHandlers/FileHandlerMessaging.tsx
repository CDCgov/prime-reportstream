import React, { useEffect } from "react";

import {
    formattedDateFromTimestamp,
    timeZoneAbbreviated,
} from "../../utils/DateTimeUtils";
import { StaticAlert } from "../StaticAlert";
import { ResponseError } from "../../network/api/WatersApi";

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
    errors: ResponseError[];
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
        errors.forEach((error: ResponseError) => {
            if (error.details) {
                console.error(`${handlerType} failure: ${error.details}`);
            }
        });
    }, [errors, handlerType]);

    return (
        <>
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
                <>
                    <h3>Errors</h3>
                    <table className="usa-table usa-table--borderless">
                        <thead>
                            <tr>
                                <th>Requested Edit</th>
                                <th>Areas Containing the Requested Edit</th>
                                <th>Field</th>
                                <th>Tracking ID(s)</th>
                            </tr>
                        </thead>
                        <tbody>
                            {errors.map((e, i) => {
                                return <ErrorRow error={e} index={i} />;
                            })}
                        </tbody>
                    </table>
                </>
            )}
        </>
    );
};

interface FileWarningBannerProps {
    message: string;
}

export const FileWarningBanner = ({ message }: FileWarningBannerProps) => {
    return <StaticAlert type={"warning"} heading="Warning" message={message} />;
};

type FileWarningsDisplayProps = {
    warnings: ResponseError[];
    message: string;
    heading: string;
};

export const FileWarningsDisplay = ({
    warnings,
    message,
    heading,
}: FileWarningsDisplayProps) => {
    return (
        <>
            <StaticAlert type={"warning"} heading={heading} message={message} />
            <h3>Warnings</h3>
            <table className="usa-table usa-table--borderless">
                <thead>
                    <tr>
                        <th>Warning</th>
                        <th>Indices</th>
                        <th>Field</th>
                        <th>Tracking ID(s)</th>
                    </tr>
                </thead>
                <tbody>
                    {warnings.map((w, i) => {
                        return <ErrorRow error={w} index={i} />;
                    })}
                </tbody>
            </table>
        </>
    );
};

interface ErrorRowProps {
    error: ResponseError;
    index: number;
}

const ErrorRow = ({ error, index }: ErrorRowProps) => {
    const { message, indices, field, trackingIds } = error;
    return (
        <tr key={"error_" + index}>
            <td>{truncateErrorMesssage(message)}</td>
            <td>
                {indices?.length && indices.length > 0 && (
                    <span>Row(s): {indices.join(", ")}</span>
                )}
            </td>
            <td>{field}</td>
            <td>
                {trackingIds?.length && trackingIds.length > 0 && (
                    <span>{trackingIds.join(", ")}</span>
                )}
            </td>
        </tr>
    );
};
