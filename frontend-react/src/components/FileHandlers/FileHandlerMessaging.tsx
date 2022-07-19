import React, { useEffect } from "react";

import { StaticAlert } from "../StaticAlert";
import { FileResponseError } from "../../network/api/WatersApi";

type FileSuccessDisplayProps = {
    fileName: string;
    destinations: string;
    heading: string;
    message: string;
    showDestinations: boolean;
};

export const FileSuccessDisplay = ({
    fileName,
    destinations,
    heading,
    message,
    showDestinations,
}: FileSuccessDisplayProps) => {
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
                {showDestinations && (
                    <div>
                        <p className="text-normal text-base margin-bottom-0">
                            Recipients
                        </p>
                        <p className="margin-top-05">{destinationsDisplay}</p>
                    </div>
                )}
            </div>
        </>
    );
};

type FileErrorDisplayProps = {
    errors: FileResponseError[];
    messageText: string;
    fileName: string;
    errorType: string;
};

/***
 * This function attempts to truncate an error message if it contains
 * a full stack trace
 * @param truncateErrorMessage - the error message to potentially reformat
 * @returns - the original or transformed error message
 */
const reformat = (truncateErrorMessage: string | undefined): string => {
    if (!truncateErrorMessage) return "";

    if (
        truncateErrorMessage.includes("\n") &&
        truncateErrorMessage.includes("Exception:")
    )
        return (
            truncateErrorMessage.substring(
                0,
                truncateErrorMessage.indexOf("\n")
            ) + " ..."
        );

    return truncateErrorMessage;
};

export const FileErrorDisplay = ({
    fileName,
    errors,
    messageText,
    errorType,
}: FileErrorDisplayProps) => {
    const showErrorTable =
        errors && errors.length && errors.some((error) => error.message);

    useEffect(() => {
        errors.forEach((error: FileResponseError) => {
            if (error.details) {
                console.error(`${errorType} failure: ${error.details}`);
            }
        });
    }, [errors, errorType]);

    return (
        <div>
            <StaticAlert
                type={"error"}
                heading={"We found errors in your file."}
                message={messageText}
            />
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
                                    <td>{reformat(e.message)}</td>
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
