import React, { useEffect, useMemo } from "react";
import { Icon, Tooltip } from "@trussworks/react-uswds";

import {
    formattedDateFromTimestamp,
    timeZoneAbbreviated,
} from "../../utils/DateTimeUtils";
import { StaticAlert, StaticAlertType } from "../StaticAlert";
import {
    ErrorCodeTranslation,
    ResponseError,
} from "../../config/endpoints/waters";
import { Destination } from "../../resources/ActionDetailsResource";
import { USExtLink, USLink } from "../USLink";

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
                type={[StaticAlertType.Success, StaticAlertType.Slim]}
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
                                    <USLink href={`/submissions/${reportId}`}>
                                        {reportId}
                                    </USLink>
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

export enum RequestLevel {
    WARNING = "Warnings",
    ERROR = "Errors",
}

const TrackingIDTooltip = () => {
    return (
        <Tooltip
            className="fixed-tooltip"
            position="right"
            label={"Defaults to MSH-10"}
        >
            <Icon.Help />
        </Tooltip>
    );
};

type RequestedChangesDisplayProps = {
    title: RequestLevel;
    data: ResponseError[];
    message: string;
    heading: string;
    handlerType: string;
};

export const RequestedChangesDisplay = ({
    title,
    data,
    message,
    heading,
    handlerType,
}: RequestedChangesDisplayProps) => {
    const alertType = useMemo(
        () =>
            title === RequestLevel.WARNING
                ? StaticAlertType.Warning
                : StaticAlertType.Error,
        [title]
    );
    const showTable =
        data &&
        data.length &&
        data.some((responseItem) => responseItem.message);

    useEffect(() => {
        data.forEach((error: ResponseError) => {
            if (title === RequestLevel.ERROR && error.details) {
                console.error(`${handlerType} failure: ${error.details}`);
            }
        });
    }, [data, handlerType, title]);

    return (
        <>
            <StaticAlert type={alertType} heading={heading} message={message}>
                <h5 className="margin-bottom-1">Resources</h5>
                <ul className={"margin-0"}>
                    <li>
                        <USLink
                            target="_blank"
                            href="/resources/programmers-guide"
                        >
                            ReportStream Programmers Guide
                        </USLink>
                    </li>
                    <li>
                        <USExtLink href="https://www.cdc.gov/csels/dls/sars-cov-2-livd-codes.html">
                            LOINC In Vitro Diagnostic (LIVD) Test Code Mapping
                        </USExtLink>
                    </li>
                </ul>
            </StaticAlert>
            {showTable && (
                <>
                    <h3>{title}</h3>
                    <table
                        className="usa-table usa-table--borderless rs-width-100"
                        data-testid="error-table"
                    >
                        <thead>
                            <tr>
                                <th className="rs-table-column-minwidth">
                                    Requested Edit
                                </th>
                                <th className="rs-table-column-minwidth">
                                    Field
                                </th>
                                <th className="rs-table-column-minwidth">
                                    Tracking ID(s) <TrackingIDTooltip />
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            {data.map((e, i) => {
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
    return (
        <StaticAlert
            type={StaticAlertType.Warning}
            heading="Warning"
            message={message}
        />
    );
};

interface ErrorRowProps {
    error: ResponseError;
    index: number;
}

const ErrorRow = ({ error, index }: ErrorRowProps) => {
    const { message, field, errorCode, trackingIds } = error;
    return (
        <tr key={"error_" + index}>
            <td>
                {errorCode &&
                errorCode !== "UNKNOWN" &&
                ErrorCodeTranslation[errorCode]
                    ? ErrorCodeTranslation[errorCode]
                    : message}
            </td>
            <td className="rs-table-column-minwidth">{field}</td>
            <td className="rs-table-column-minwidth">
                {trackingIds?.length && trackingIds.length > 0 && (
                    <span>{trackingIds.join(", ")}</span>
                )}
            </td>
        </tr>
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
                type={[StaticAlertType.Error, StaticAlertType.Slim]}
                heading={heading}
                message={message}
            />
            <h3>Jurisdictions</h3>
            {destinations?.map((d) => (
                <React.Fragment key={d.organization_id}>
                    <table
                        className="usa-table usa-table--borderless width-full"
                        data-testid="error-table"
                    >
                        <thead>
                            <tr className="text-baseline">
                                <th>
                                    {d.organization} <br />
                                    <span className="font-sans-3xs text-normal">
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
