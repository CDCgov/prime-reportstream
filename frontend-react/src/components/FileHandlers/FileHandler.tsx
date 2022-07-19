import React, { useState, useMemo } from "react";

import { showError } from "../AlertNotifications";
import { useSessionContext } from "../../contexts/SessionContext";
import { useOrganizationResource } from "../../hooks/UseOrganizationResouce";
import { FileResponseError } from "../../network/api/WatersApi";
import { WatersPost } from "../../network/api/WatersApiFunctions";
import { Destination } from "../../resources/ActionDetailsResource";
import Spinner from "../Spinner"; // TODO: refactor to use suspense

import { FileErrorDisplay, FileSuccessDisplay } from "./FileHandlerMessaging";
import { FileHandlerForm } from "./FileHandlerForm";

// values taken from Report.kt
const PAYLOAD_MAX_BYTES = 50 * 1000 * 1000; // no idea why this isn't in "k" (* 1024).
const REPORT_MAX_ITEMS = 10000;
const REPORT_MAX_ITEM_COLUMNS = 2000;

const parseCsvForError = (fileName: string, filecontent: string) => {
    const error = false;
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

interface FileHandlerProps {
    headingText: string;
    action: string;
    fetcher: WatersPost;
    successMessage: string;
    formLabel: string;
    resetText: string;
    submitText: string;
    showDestinations: boolean;
}

const FileHandler = ({
    headingText,
    action,
    fetcher,
    successMessage,
    formLabel,
    resetText,
    submitText,
    showDestinations,
}: FileHandlerProps) => {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [fileInputResetValue, setFileInputResetValue] = useState(0);
    const [fileContent, setFileContent] = useState("");
    const [contentType, setContentType] = useState("");
    const [fileType, setFileType] = useState("");
    const [fileName, setFileName] = useState("");
    const [errors, setErrors] = useState<FileResponseError[]>([]);
    const [destinations, setDestinations] = useState("");
    const [reportId, setReportId] = useState<string | null>(null);
    const [cancellable, setCancellable] = useState<boolean>(false);
    const [errorMessageText, setErrorMessageText] = useState(
        "Please resolve the errors below and upload your edited file. Your file has not been accepted."
    );

    const { memberships, oktaToken } = useSessionContext();
    const { organization } = useOrganizationResource();

    const accessToken = oktaToken?.accessToken;
    const parsedName = memberships.state.active?.parsedName;
    const senderName = memberships.state.active?.senderName;
    const client = `${parsedName}.${senderName}`;

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
                setContentType("text/csv");
                if (parseCsvForError(file.name, filecontent)) {
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
            response = await fetcher(
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
                        .map((d: Destination) => d.organization)
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
            const FileResponseErrors = response.errors.map((error: any) => {
                const rowList =
                    error.indices && error.indices.length > 0
                        ? error.indices.join(", ")
                        : "";
                return { ...error, rowList };
            });
            setErrors(FileResponseErrors);
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
            <h1 className="margin-top-0 margin-bottom-5">{headingText}</h1>
            <h2 className="font-sans-lg">{organization?.description}</h2>
            {reportId && (
                <FileSuccessDisplay
                    fileName={fileName}
                    destinations={destinations}
                    heading={successMessage}
                    message={`Your file meets the standard ${fileType} schema and can be successfully transmitted.`}
                    showDestinations={showDestinations}
                />
            )}

            {errors.length > 0 && (
                <FileErrorDisplay
                    fileName={fileName}
                    errorType={action}
                    errors={errors}
                    messageText={errorMessageText}
                />
            )}
            {isSubmitting && (
                <div className="grid-col flex-1 display-flex flex-column flex-align-center">
                    <div className="grid-row">
                        <Spinner />
                    </div>
                    <div className="grid-row">Processing file...</div>
                </div>
            )}
            {!isSubmitting && (
                <FileHandlerForm
                    handleSubmit={handleSubmit}
                    handleFileChange={handleFileChange}
                    resetState={resetState}
                    fileInputResetValue={fileInputResetValue}
                    submitted={submitted}
                    cancellable={cancellable}
                    fileName={fileName}
                    formLabel={formLabel}
                    resetText={resetText}
                    submitText={submitText}
                />
            )}
        </div>
    );
};

export default FileHandler;
