import React, { useState, useMemo } from "react";

import { showError } from "../AlertNotifications";
import { useSessionContext } from "../../contexts/SessionContext";
import { useOrganizationResource } from "../../hooks/UseOrganizationResouce";
import { ResponseError } from "../../network/api/WatersApi";
import { WatersPost } from "../../network/api/WatersApiFunctions";
import { Destination } from "../../resources/ActionDetailsResource";
import Spinner from "../Spinner"; // TODO: refactor to use suspense

import {
    FileErrorDisplay,
    FileSuccessDisplay,
    FileWarningsDisplay,
    FileWarningBanner,
} from "./FileHandlerMessaging";
import { FileHandlerForm } from "./FileHandlerForm";

// values taken from Report.kt
const PAYLOAD_MAX_BYTES = 50 * 1000 * 1000; // no idea why this isn't in "k" (* 1024).
const REPORT_MAX_ITEMS = 10000;
const REPORT_MAX_ITEM_COLUMNS = 2000;

const SERVER_ERROR_MESSAGING = {
    heading: "Error",
    message: "There was a server error. Your file has not been accepted.",
};

const errorMessagingMap = {
    validation: {
        server: SERVER_ERROR_MESSAGING,
        file: {
            heading: "Your file has not passed validation",
            message: "Please review the errors below.",
        },
    },
    upload: {
        server: SERVER_ERROR_MESSAGING,
        file: {
            heading: "We found errors in your file",
            message:
                "Please resolve the errors below and upload your edited file. Your file has not been accepted.",
        },
    },
};

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

export enum FileHandlerType {
    UPLOAD = "upload",
    VALIDATION = "validation",
}

enum ErrorType {
    SERVER = "server",
    FILE = "file",
}

interface FileHandlerProps {
    headingText: string;
    handlerType: FileHandlerType;
    fetcher: WatersPost;
    successMessage: string;
    formLabel: string;
    resetText: string;
    submitText: string;
    showSuccessMetadata: boolean;
    showWarningBanner: boolean;
    warningText?: string;
}

const FileHandler = ({
    headingText,
    handlerType,
    fetcher,
    successMessage,
    formLabel,
    resetText,
    submitText,
    showSuccessMetadata,
    showWarningBanner,
    warningText,
}: FileHandlerProps) => {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [fileInputResetValue, setFileInputResetValue] = useState(0);
    const [fileContent, setFileContent] = useState("");
    const [contentType, setContentType] = useState("");
    const [fileType, setFileType] = useState("");
    const [fileName, setFileName] = useState("");
    const [errors, setErrors] = useState<ResponseError[]>([]);
    const [destinations, setDestinations] = useState("");
    const [reportId, setReportId] = useState<string | null>(null);
    const [successTimestamp, setSuccessTimestamp] = useState<
        string | undefined
    >("");
    const [cancellable, setCancellable] = useState<boolean>(false);
    const [errorType, setErrorType] = useState<ErrorType>(ErrorType.FILE);
    const [warnings, setWarnings] = useState<ResponseError[]>([]);

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
        setSuccessTimestamp("");
        setCancellable(false);
        setErrorType(ErrorType.FILE);
        setWarnings([]);
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
        setSuccessTimestamp("");
        setWarnings([]);

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
                setSuccessTimestamp(response.timestamp);
                event?.currentTarget?.reset && event.currentTarget.reset();
            }

            // if there is a response status,
            // then there was most likely a server-side error as the json was not parsed
            if (response?.errors?.length && response?.status) {
                setErrorType(ErrorType.SERVER);
            }
            if (response?.warnings?.length) {
                setWarnings(response.warnings);
            }
        } catch (error) {
            // Noop.  Errors are collected below
            console.error("Unexpected error in file handler", error);
        }

        // Process the error messages
        if (response?.errors && response.errors.length > 0) {
            // Add a string to properly display the indices if available.
            setErrors(response.errors);
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

    const successDescription = useMemo(() => {
        let suffix = "";
        if (handlerType === "upload") {
            suffix = " and will be transmitted";
        }
        const schemaDescription =
            fileType === "HL7"
                ? "ReportStream standard HL7 v2.5.1"
                : "standard CSV";
        return `Your file meets the ${schemaDescription} schema${suffix}.`;
    }, [fileType, handlerType]);

    const warningDescription = useMemo(() => {
        return handlerType === "upload"
            ? "Your file has been transmitted"
            : "Your file has passed validation";
    }, [handlerType]);

    const errorMessaging = errorMessagingMap[handlerType][errorType];

    return (
        <div className="grid-container usa-section margin-bottom-10">
            <h1 className="margin-top-0 margin-bottom-5">{headingText}</h1>
            <h2 className="font-sans-lg">{organization?.description}</h2>
            {showWarningBanner && (
                <FileWarningBanner message={warningText || ""} />
            )}
            {warnings.length > 0 && (
                <FileWarningsDisplay
                    warnings={warnings}
                    heading="We found non-critical issues in your file"
                    message={`The following warnings were returned while processing your file. ${warningDescription}, but these warning areas can be addressed to enhance clarity."`}
                />
            )}
            {reportId && (
                <FileSuccessDisplay
                    fileName={fileName}
                    extendedMetadata={{
                        destinations,
                        timestamp: successTimestamp,
                        reportId,
                    }}
                    heading={successMessage}
                    message={successDescription}
                    showExtendedMetadata={showSuccessMetadata}
                />
            )}
            {errors.length > 0 && (
                <FileErrorDisplay
                    fileName={fileName}
                    handlerType={handlerType}
                    errors={errors}
                    heading={errorMessaging.heading}
                    message={errorMessaging.message}
                />
            )}
            {isSubmitting && (
                <div className="grid-col flex-1 display-flex flex-column flex-align-center margin-top-4">
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
