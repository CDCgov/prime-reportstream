import React, { useState, useMemo, useEffect } from "react";
import {
    Button,
    Form,
    FormGroup,
    Label,
    FileInput,
} from "@trussworks/react-uswds";

import { showError } from "../components/AlertNotifications";
import Spinner from "../components/Spinner";
import watersApiFunctions from "../network/api/WatersApiFunctions";
import { useSessionContext } from "../contexts/SessionContext";
import { StaticAlert } from "../components/StaticAlert";
import { useOrganizationResource } from "../hooks/UseOrganizationResouce";
import { ResponseError } from "../network/api/WatersApi";

interface ValidationError extends ResponseError {
    rowList?: string;
}
// values taken from Report.kt
const PAYLOAD_MAX_BYTES = 50 * 1000 * 1000; // no idea why this isn't in "k" (* 1024).
const REPORT_MAX_ITEMS = 10000;
const REPORT_MAX_ITEM_COLUMNS = 2000;
// const REPORT_MAX_ERRORS = 100;

const Validate = () => {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [fileInputResetValue, setFileInputResetValue] = useState(0);
    const [fileContent, setFileContent] = useState("");
    const [contentType, setContentType] = useState("");
    const [fileType, setFileType] = useState("");
    const [fileName, setFileName] = useState("");
    const [errors, setErrors] = useState<ValidationError[]>([]);
    const [destinations, setDestinations] = useState("");
    const [reportId, setReportId] = useState(null);
    const [cancellable, setCancellable] = useState<boolean>(false);
    const [errorMessageText, setErrorMessageText] = useState(
        "Please resolve the errors below and upload your edited file. Your file has not been accepted."
    );

    const { memberships, oktaToken } = useSessionContext();

    const resetState = () => {
        setIsSubmitting(false);
        setFileInputResetValue(fileInputResetValue + 1);
        setFileContent("");
        setContentType("");
        setFileType("");
        setFileName("");
        setErrors([]);
        setDestinations("");
        setReportId(null);
        setCancellable(false);
        setErrorMessageText(
            "Please resolve the errors below and upload your edited file. Your file has not been accepted."
        );
    };

    const accessToken = oktaToken?.accessToken;
    const parsedName = memberships.state.active?.parsedName;
    const senderName = memberships.state.active?.senderName;
    const client = `${parsedName}.${senderName}`;

    const { organization } = useOrganizationResource();

    const handleFileChange = async (
        event: React.ChangeEvent<HTMLInputElement>
    ) => {
        try {
            if (!event?.currentTarget?.files?.length) {
                // no files selected
                return;
            }
            const file = event.currentTarget.files.item(0);
            if (!file) {
                // shouldn't happen but keeps linter happy
                return;
            }

            let uploadType;
            if (file.type) {
                uploadType = file.type;
            } else {
                // look at the filename extension.
                // it's all we have to go off of for now
                const fileNameArray = file.name.split(".");
                uploadType = fileNameArray[fileNameArray.length - 1];
            }

            if (
                uploadType !== "text/csv" &&
                uploadType !== "csv" &&
                uploadType !== "hl7"
            ) {
                showError(`The file type must be .csv or .hl7`);
                return;
            }
            setFileType(uploadType.match("hl7") ? "HL7" : "CSV");

            if (file.size > PAYLOAD_MAX_BYTES) {
                const maxkbytes = (PAYLOAD_MAX_BYTES / 1024).toLocaleString(
                    "en-US",
                    { maximumFractionDigits: 2, minimumFractionDigits: 2 }
                );

                showError(
                    `The file '${file.name}' is too large. The maximum file size is ${maxkbytes}k`
                );
                return;
            }
            // load the "contents" of the file. Hope it fits in memory!
            const filecontent = await file.text();

            if (uploadType === "csv" || uploadType === "text/csv") {
                if (basicCsvFileValidationError(file.name, filecontent)) {
                    return;
                }
            } else {
                // todo: do any front-end validations we can do here on hl7 files before it hits the server here
                setContentType("application/hl7-v2");
            }

            setFileName(file.name);
            setFileContent(filecontent);
            setCancellable(true);
        } catch (err: any) {
            // todo: have central error reporting mechanism.
            console.error(err);
            showError(`An unexpected error happened: '${err.toString()}'`);
            setCancellable(false);
        }
    };

    const basicCsvFileValidationError = (
        fileName: string,
        filecontent: string
    ) => {
        const error = false;
        setContentType("text/csv");
        // count the number of lines
        const linecount = (filecontent.match(/\n/g) || []).length + 1;
        if (linecount > REPORT_MAX_ITEMS) {
            showError(
                `The file '${fileName}' has too many rows. The maximum number of rows allowed is ${REPORT_MAX_ITEMS}.`
            );
            return true;
        }
        if (linecount <= 1) {
            showError(
                `The file '${fileName}' doesn't contain any valid data. ` +
                    `File should have a header line and at least one line of data.`
            );
            return true;
        }

        // get the first line and examine it
        const firstline = (filecontent.match(/^(.*)\n/) || [""])[0];
        // ideally, the columns would be comma seperated, but they may be tabs, because the first
        // line is a header, we don't have to worry about escaped delimiters in strings (e.g. ,"Smith, John",)
        const columncount =
            (firstline.match(/,/g) || []).length ||
            (firstline.match(/\t/g) || []).length;

        if (columncount > REPORT_MAX_ITEM_COLUMNS) {
            showError(
                `The file '${fileName}' has too many columns. The maximum number of allowed columns is ${REPORT_MAX_ITEM_COLUMNS}.`
            );
            return true;
        }
        // todo: this is a good place to do basic validation of the upload file. e.g. does it have
        // all the required columns? Are any rows obviously not correct (empty or obviously wrong type)?
        return error;
    };

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        // reset the state on subsequent uploads
        setIsSubmitting(true);
        setReportId(null);
        setErrors([]);
        setDestinations("");

        if (fileContent.length === 0) {
            return;
        }

        let response;
        try {
            response = await watersApiFunctions.postData(
                client,
                fileName,
                contentType,
                fileContent,
                parsedName || "",
                accessToken || ""
            );

            if (response?.destinations?.length) {
                // NOTE: `{ readonly [key: string]: string }` means a key:value object
                setDestinations(
                    response.destinations
                        .map(
                            (d: { readonly [key: string]: string }) =>
                                d["organization"]
                        )
                        .join(", ")
                );
            }

            if (response?.id) {
                setReportId(response.id);
                event.currentTarget.reset();
            }

            if (response?.errors?.length) {
                // if there is a response status,
                // then there was most likely a server-side error as the json was not parsed
                setErrorMessageText(
                    response?.status
                        ? "There was a server error. Your file has not been accepted."
                        : "Please resolve the errors below and upload your edited file. Your file has not been accepted."
                );
            }
        } catch (error) {
            // Noop.  Errors are collected below
        }

        // Process the error messages
        if (response?.errors && response.errors.length > 0) {
            // Add a string to properly display the indices if available.
            const validationErrors = response.errors.map((error: any) => {
                const rowList =
                    error.indices && error.indices.length > 0
                        ? error.indices.join(", ")
                        : "";
                return { ...error, rowList };
            });
            setErrors(validationErrors);
            setCancellable(true);
        } else {
            setCancellable(false);
        }
        // Changing the key to force the FileInput to reset.
        // Otherwise it won't recognize changes to the file's content unless the file name changes
        setFileInputResetValue(fileInputResetValue + 1);
        setIsSubmitting(false);
    };

    const submitted = useMemo(
        () => !!(reportId || errors.length),
        [reportId, errors.length]
    );

    return (
        <div className="grid-container usa-section margin-bottom-10">
            <h1 className="margin-top-0 margin-bottom-5">File Validator</h1>
            <h2 className="font-sans-lg">{organization?.description}</h2>
            {reportId && (
                <ValidationSuccessDisplay
                    fileName={fileName}
                    fileType={fileType}
                    destinations={destinations}
                />
            )}

            {errors.length > 0 && (
                <ValidationErrorDisplay
                    fileName={fileName}
                    fileType={fileType}
                    errors={errors}
                    messageText={errorMessageText}
                />
            )}

            <Form
                onSubmit={(e) => handleSubmit(e)}
                className="rs-full-width-form"
            >
                {!submitted && (
                    <FormGroup className="margin-bottom-3">
                        <Label
                            className="font-sans-xs"
                            id="upload-csv-input-label"
                            htmlFor="upload-csv-input"
                        >
                            Select an HL7 or CSV formatted file to validate.
                        </Label>
                        <FileInput
                            key={fileInputResetValue}
                            id="upload-csv-input"
                            name="upload-csv-input"
                            aria-describedby="upload-csv-input-label"
                            onChange={(e) => handleFileChange(e)}
                            required
                        />
                    </FormGroup>
                )}
                {isSubmitting && (
                    <div className="grid-col flex-1 display-flex flex-column flex-align-center">
                        <div className="grid-row">
                            <Spinner />
                        </div>
                        <div className="grid-row">Processing file...</div>
                    </div>
                )}
                <div className="grid-row">
                    <div className="grid-col flex-1 display-flex flex-column flex-align-start">
                        {cancellable && !isSubmitting && (
                            <Button onClick={resetState} type="button" outline>
                                <span>Cancel</span>
                            </Button>
                        )}
                    </div>
                    <div className="grid-col flex-1" />
                    <div className="grid-col flex-1 display-flex flex-column flex-align-end">
                        <SubmitButton
                            isSubmitting={isSubmitting}
                            submitted={submitted}
                            fileName={fileName}
                            reset={resetState}
                        />
                    </div>
                </div>
            </Form>
        </div>
    );
};

type ValidationSuccessDisplayProps = {
    fileName: string;
    fileType: string;
    destinations: string;
};

const ValidationSuccessDisplay = ({
    fileName,
    fileType,
    destinations,
}: ValidationSuccessDisplayProps) => {
    const destinationsDisplay =
        destinations || "There are no known recipients at this time.";
    return (
        <>
            <StaticAlert
                type={"success"}
                heading={"Your file has been validated"}
                message={`Your file meets the standard ${fileType} schema and can be successfully transmitted.`}
            />
            <div>
                <p
                    id="validatedFilename"
                    className="text-normal text-base margin-bottom-0"
                >
                    File name
                </p>
                <p className="margin-top-05">{fileName}</p>
                <div>
                    <p className="text-normal text-base margin-bottom-0">
                        Recipients
                    </p>
                    <p className="margin-top-05">{destinationsDisplay}</p>
                </div>
            </div>
        </>
    );
};

type ValidationErrorDisplayProps = {
    fileType: string;
    errors: ValidationError[];
    messageText: string;
    fileName: string;
};

const ValidationErrorDisplay = ({
    fileName,
    errors,
    messageText,
}: ValidationErrorDisplayProps) => {
    const showErrorTable =
        errors && errors.length && errors.some((error) => error.message);

    useEffect(() => {
        errors.forEach((error: ValidationError) => {
            if (error.details) {
                console.error("Validation Failed: ", error.details);
            }
        });
    }, [errors]);

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
                                    <td>{e.message}</td>
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

type SubmitButtonProps = {
    isSubmitting: boolean;
    submitted: boolean;
    fileName: string;
    reset: () => void;
};

const SubmitButton = ({
    isSubmitting,
    submitted,
    fileName,
    reset,
}: SubmitButtonProps) => {
    if (isSubmitting) {
        return null;
    }
    if (submitted) {
        return (
            <Button type="button" onClick={reset}>
                Validate another file
            </Button>
        );
    }
    return (
        <Button type="submit" disabled={fileName.length === 0}>
            Validate
        </Button>
    );
};

export default Validate;
