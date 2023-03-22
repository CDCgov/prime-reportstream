import React, { ReactNode } from "react";
import { Button, Icon } from "@trussworks/react-uswds";

import {
    formattedDateFromTimestamp,
    timeZoneAbbreviated,
} from "../../utils/DateTimeUtils";
import { StaticAlert, StaticAlertType } from "../StaticAlert";
import { ErrorCode, ResponseError } from "../../config/endpoints/waters";
import { Destination } from "../../resources/ActionDetailsResource";
import { USLink, USExtLink } from "../USLink";
import { FileType } from "../../utils/TemporarySettingsAPITypes";
import { saveToCsv } from "../../utils/FileUtils";

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
                {/* TODO: can probably remove since it's not being used now */}

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

type RequestedChangesDisplayProps = {
    title: RequestLevel;
    data: ResponseError[];
    message: string;
    heading: string;
    schemaColumnHeader: string;
};

export const RequestedChangesDisplay = ({
    title,
    data,
    message,
    heading,
    schemaColumnHeader,
}: RequestedChangesDisplayProps) => {
    const alertType =
        title === RequestLevel.WARNING
            ? StaticAlertType.Warning
            : StaticAlertType.Error;

    const showTable =
        data &&
        data.length &&
        data.some((responseItem) => responseItem.message);

    function handleSaveToCsvClick() {
        return saveToCsv(data, {
            filename:
                `${schemaColumnHeader}_${alertType}s_${new Date().getTime()}`.toLowerCase(),
        });
    }

    return (
        <div>
            <StaticAlert type={alertType} heading={heading} message={message} />

            {showTable && (
                <div className="padding-y-4">
                    <div className="margin-bottom-4 display-flex flex-justify flex-align-center">
                        <h3 className="margin-0">{title}</h3>

                        <Button
                            className="usa-button usa-button--outline display-flex flex-align-center"
                            type="button"
                            onClick={handleSaveToCsvClick}
                        >
                            Download edits as CSV <Icon.FileDownload />
                        </Button>
                    </div>

                    <div className="padding-x-4 padding-y-2 radius-md bg-base-lightest">
                        <table
                            className="usa-table usa-table--borderless"
                            data-testid="error-table"
                        >
                            <thead>
                                <tr>
                                    <th className="rs-table-column-minwidth">
                                        Recommended Edit
                                    </th>
                                    <th className="rs-table-column-minwidth">
                                        {schemaColumnHeader} Row
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
                                            schemaColumnHeader={
                                                schemaColumnHeader
                                            }
                                        />
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
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

const HL7_PRODUCT_MATRIX_URL =
    "https://www.hl7.org/implement/standards/product_brief.cfm";
const CDC_LIVD_CODES_URL = "https://www.cdc.gov/csels/dls/livd-codes.html";

export type ValidationErrorMessageProps = {
    errorCode: ErrorCode;
    field?: string;
    message?: string;
};

export function ValidationErrorMessage({
    errorCode,
    field,
    message,
}: ValidationErrorMessageProps) {
    let child: ReactNode;

    switch (errorCode) {
        case ErrorCode.INVALID_MSG_PARSE_BLANK:
            child = (
                <>
                    Blank message(s) found within file. Blank messages cannot be
                    processed.
                </>
            );
            break;
        case ErrorCode.INVALID_HL7_MSG_TYPE_MISSING:
            child = (
                <>
                    Missing required HL7 message type field MSH-9. Fill in the
                    blank field before resubmitting.
                </>
            );
            break;
        case ErrorCode.INVALID_HL7_MSG_TYPE_UNSUPPORTED:
            child = (
                <>
                    We found an unsupported HL7 message type. Please reformat to
                    ORU-RO1. Refer to{" "}
                    <USExtLink href={HL7_PRODUCT_MATRIX_URL}>
                        HL7 specification
                    </USExtLink>{" "}
                    for more details.
                </>
            );
            break;
        case ErrorCode.INVALID_HL7_MSG_FORMAT_INVALID:
            child = (
                <>
                    Invalid HL7 message format. Check your formatting by
                    referring to{" "}
                    <USExtLink href={HL7_PRODUCT_MATRIX_URL}>
                        HL7 specification
                    </USExtLink>
                    .
                </>
            );
            break;
        case ErrorCode.INVALID_MSG_PARSE_DATETIME:
            child = <>Reformat {field} as YYYYMMDDHHMM[SS[.S[S[S[S]+/-ZZZZ.</>;
            break;
        case ErrorCode.INVALID_MSG_PARSE_TELEPHONE:
            child = (
                <>
                    Reformat phone number to a 10-digit phone number (e.g. (555)
                    555-5555).
                </>
            );
            break;
        case ErrorCode.INVALID_HL7_MSG_VALIDATION:
            child = (
                <>
                    Reformat {field} to{" "}
                    <USExtLink href={HL7_PRODUCT_MATRIX_URL}>
                        HL7 specification
                    </USExtLink>
                    .
                </>
            );
            break;
        case ErrorCode.INVALID_MSG_MISSING_FIELD:
            child = <>Fill in the required field {field}.</>;
            break;
        case ErrorCode.INVALID_MSG_EQUIPMENT_MAPPING:
            child = (
                <>
                    Reformat field {field}. Refer to{" "}
                    <USExtLink href={CDC_LIVD_CODES_URL}>
                        CDC LIVD table LOINC mapping spreadsheet
                    </USExtLink>{" "}
                    for acceptable values.
                </>
            );
            break;
        default:
            child = <>{message || ""}</>;
            break;
    }

    return <p data-testid="ValidationErrorMessage">{child}</p>;
}

interface ErrorRowProps {
    error: ResponseError;
    index: number;
    schemaColumnHeader: string;
}

const ErrorRow = ({ error, index, schemaColumnHeader }: ErrorRowProps) => {
    const { errorCode, field, message, trackingIds, indices } = error;
    return (
        <tr key={"error_" + index}>
            <td>
                <ValidationErrorMessage
                    errorCode={errorCode}
                    field={field}
                    message={message}
                />
            </td>
            <td className="rs-table-column-minwidth">
                {schemaColumnHeader === FileType.CSV && indices?.length && (
                    <span>{indices.join(" + ")}</span>
                )}
                {schemaColumnHeader === FileType.HL7 && trackingIds?.length && (
                    <span>{trackingIds.join(" + ")}</span>
                )}
                {!indices?.length && !trackingIds?.length && (
                    <span>Not applicable</span>
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
