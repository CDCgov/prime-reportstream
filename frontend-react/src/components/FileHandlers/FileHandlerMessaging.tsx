import React, { ReactNode } from "react";
import { Button, Icon } from "@trussworks/react-uswds";
import { renderToString } from "react-dom/server";

import { StaticAlert, StaticAlertType } from "../StaticAlert";
import { ErrorCode, ResponseError } from "../../config/endpoints/waters";
import { Destination } from "../../resources/ActionDetailsResource";
import { USExtLink } from "../USLink";
import { FileType } from "../../utils/TemporarySettingsAPITypes";
import { saveToCsv } from "../../utils/FileUtils";
import { removeHTMLFromString } from "../../utils/misc";

const HL7_PRODUCT_MATRIX_URL =
    "https://www.hl7.org/implement/standards/product_brief.cfm";
const CDC_LIVD_CODES_URL = "https://www.cdc.gov/csels/dls/livd-codes.html";

export enum RequestLevel {
    WARNING = "Warnings",
    ERROR = "Errors",
}

type RequestedChangesDisplayProps = {
    title: RequestLevel;
    data: ResponseError[];
    message: string;
    heading: string;
    schemaColumnHeader: FileType;
    file?: File;
};

/**
 * Given a filename and the alert type, generate a safe filename for the errors/warnings CSV
 *
 * @param originalFileName
 * @param requestLevel
 */
export function getSafeFileName(
    originalFileName: string,
    requestLevel: RequestLevel,
) {
    const joinedStr = [originalFileName, requestLevel].join("-").toLowerCase();

    return joinedStr.replace(/[^a-z0-9]/gi, "-");
}

export const RequestedChangesDisplay = ({
    title,
    data,
    message,
    heading,
    schemaColumnHeader,
    file,
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
        // Since the detailed error code messaging is stored on the client,
        // and contains HTML markup, we need to stringify the React component
        // with renderToString. We then strip out the HTML markup with Regex
        // for readability within the CSV.
        const dataWithErrorMessage = data.map((item) => {
            return {
                ...item,
                errorMessageDetails: removeHTMLFromString(
                    renderToString(
                        <ValidationErrorMessage
                            errorCode={item.errorCode}
                            field={item.field}
                            message={item.message}
                        />,
                    ),
                ),
            };
        });
        return saveToCsv(dataWithErrorMessage, {
            filename: getSafeFileName(file?.name || "", title),
        });
    }

    return (
        <div>
            <StaticAlert type={alertType} heading={heading} message={message} />

            {showTable && (
                <div className="padding-y-4">
                    <div className="margin-bottom-4 display-flex flex-justify flex-align-center">
                        <p className="validation-section-header">{title}</p>

                        <Button
                            className="usa-button usa-button--outline display-flex flex-align-center margin-0"
                            type="button"
                            onClick={handleSaveToCsvClick}
                        >
                            Download edits as CSV{" "}
                            <Icon.FileDownload
                                className="margin-left-1"
                                size={3}
                            />
                        </Button>
                    </div>

                    <div className="padding-4 radius-md bg-base-lightest">
                        <table
                            className="usa-table usa-table--borderless"
                            data-testid="error-table"
                        >
                            <thead>
                                <tr>
                                    <th className="rs-table-column-minwidth">
                                        {title === RequestLevel.WARNING &&
                                            "Recommended"}{" "}
                                        {title === RequestLevel.ERROR &&
                                            "Required"}{" "}
                                        edit
                                    </th>
                                    <th className="rs-table-column-minwidth">
                                        {schemaColumnHeader === FileType.CSV &&
                                            "CSV row"}
                                        {schemaColumnHeader === FileType.HL7 &&
                                            "MSH 10"}
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

    return (
        <p className="margin-0" data-testid="ValidationErrorMessage">
            {child}
        </p>
    );
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
    function handleJurisdictionSaveToCsvClick() {
        function reformatJurisdictionData() {
            for (const item of destinations || []) {
                return item.filteredReportRows.map((row) => {
                    return { jurisdiction: item.organization, errorCode: row };
                });
            }
        }
        return saveToCsv(reformatJurisdictionData(), {
            filename: "jurisdictional-required-edits",
        });
    }
    return (
        <>
            <StaticAlert
                type={StaticAlertType.Warning}
                heading={heading}
                message={message}
            />

            <div className="padding-y-4">
                <div className="display-flex flex-justify flex-align-center">
                    <p className="validation-section-header">Jurisdictions</p>

                    <Button
                        className="usa-button usa-button--outline display-flex flex-align-center margin-0"
                        type="button"
                        onClick={handleJurisdictionSaveToCsvClick}
                    >
                        Download edits as CSV{" "}
                        <Icon.FileDownload className="margin-left-1" size={3} />
                    </Button>
                </div>
            </div>

            {destinations?.map((d, idx) => (
                <div className="padding-4 radius-md bg-base-lightest margin-bottom-4">
                    <table
                        className="usa-table usa-table--borderless"
                        data-testid="error-table"
                        key={idx}
                    >
                        <thead>
                            <tr>
                                <th className="rs-table-column-minwidth">
                                    {d.organization}
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            {d.filteredReportItems.map((e, i) => {
                                return (
                                    <tr key={"error_" + i}>
                                        <td>
                                            <p
                                                className="margin-0"
                                                data-testid="ValidationErrorMessage"
                                            >
                                                {e.message}
                                            </p>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            ))}
        </>
    );
};
