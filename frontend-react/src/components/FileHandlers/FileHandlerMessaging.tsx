import React, { useEffect } from "react";
import { Link } from "react-router-dom";

import {
    formattedDateFromTimestamp,
    timeZoneAbbreviated,
} from "../../utils/DateTimeUtils";
import { capitalizeFirst } from "../../utils/misc";
import { StaticAlert } from "../StaticAlert";
import { ResponseError } from "../../config/endpoints/waters";
import { Destination } from "../../resources/ActionDetailsResource";

type ExtendedSuccessMetadata = {
    destinations?: string;
    reportId?: string;
    timestamp?: string;
};

type FileSuccessDisplayProps = {
    heading: string;
    message: string;
    showExtendedMetadata: boolean;
    extendedMetadata?: ExtendedSuccessMetadata;
};

export const FileSuccessDisplay = ({
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
            <StaticAlert
                type={"success slim"}
                heading={heading}
                message={message}
            />
            <div>
                {showExtendedMetadata && (
                    <>
                        {reportId && (
                            <div>
                                <p className="text-normal text-base margin-bottom-0">
                                    Confirmation Code
                                </p>
                                <p className="margin-top-05">
                                    <Link to={`/submissions/${reportId}`}>
                                        {reportId}
                                    </Link>
                                </p>
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
const truncateErrorMessage = (errorMessage: string | undefined): string => {
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
                    <table
                        className="usa-table usa-table--borderless"
                        data-testid="error-table"
                    >
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
                                return (
                                    <ErrorRow
                                        error={e}
                                        index={i}
                                        key={`error${i}`}
                                    />
                                );
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
            <StaticAlert
                type={"warning slim"}
                heading={heading}
                message={message}
            />
            <h3>Warnings</h3>
            <table
                className="usa-table usa-table--borderless"
                data-testid="error-table"
            >
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
                        return (
                            <ErrorRow error={w} index={i} key={`warning${i}`} />
                        );
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
            <td>{truncateErrorMessage(message)}</td>
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

interface NoSenderBannerProps {
    action: string;
    organization: string;
}

export const NoSenderBanner = ({
    action,
    organization,
}: NoSenderBannerProps) => {
    return (
        <StaticAlert
            type={"error"}
            heading={`${capitalizeFirst(action)} unavailable`}
            message={`No valid sender found for ${organization}`}
        />
    );
};

type FileQualityFilterDisplayProps = {
    destinations: Destination[] | undefined;
    message: string;
    heading: string;
};

export const FileQualityFilterDisplay = ({
    destinations,
    heading,
    message,
}: FileQualityFilterDisplayProps) => {
    return (
        <>
            <StaticAlert
                type={"error slim"}
                heading={heading}
                message={message}
            />
            <h3>Jurisdictions</h3>
            {destinations?.map((d) => (
                <React.Fragment key={d.organization_id}>
                    <table
                        className="usa-table usa-table--borderless"
                        data-testid="error-table"
                    >
                        <thead>
                            <tr>
                                <th>
                                    {d.organization}
                                    <span className="font-ui-3xs">
                                        {" "}
                                        ({d.filteredReportItems.length})
                                        record(s) filtered out
                                    </span>
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            {d.filteredReportItems.map((f, i) => {
                                return (
                                    <tr key={i}>
                                        <td> {f.message}</td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </React.Fragment>
            ))}
        </>
    );
};
