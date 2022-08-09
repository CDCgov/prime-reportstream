import React, { useEffect, useMemo, useState } from "react";

import { showError } from "../AlertNotifications";
import { useSessionContext } from "../../contexts/SessionContext";
import { useSenderResource } from "../../hooks/UseSenderResource";
import { useOrganizationResource } from "../../hooks/UseOrganizationResource";
import { WatersPost } from "../../network/api/WatersApiFunctions";
import Spinner from "../Spinner"; // TODO: refactor to use suspense
import useFileHandler, {
    FileHandlerActionType,
    ErrorType,
    FileType,
} from "../../hooks/UseFileHandler";
import { parseCsvForError } from "../../utils/FileUtils";

import {
    FileErrorDisplay,
    FileSuccessDisplay,
    FileWarningsDisplay,
    FileWarningBanner,
    NoSenderBanner,
} from "./FileHandlerMessaging";
import { FileHandlerForm } from "./FileHandlerForm";

const FileHandlerSpinner = ({ message }: { message: string }) => (
    <div className="grid-col flex-1 display-flex flex-column flex-align-center margin-top-4">
        <div className="grid-row">
            <Spinner />
        </div>
        <div className="grid-row">{message}</div>
    </div>
);

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

export enum FileHandlerType {
    UPLOAD = "upload",
    VALIDATION = "validation",
}

interface FileHandlerProps {
    headingText: string;
    handlerType: FileHandlerType;
    fetcher: WatersPost;
    successMessage: string;
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
    resetText,
    submitText,
    showSuccessMetadata,
    showWarningBanner,
    warningText,
}: FileHandlerProps) => {
    const { state, dispatch } = useFileHandler();
    const [fileContent, setFileContent] = useState("");

    const {
        isSubmitting,
        fileInputResetValue,
        contentType,
        fileType,
        fileName,
        errors,
        destinations,
        reportId,
        successTimestamp,
        cancellable,
        errorType,
        warnings,
        localError,
    } = state;

    useEffect(() => {
        if (localError) {
            showError(localError);
        }
    }, [localError]);

    const { memberships, oktaToken } = useSessionContext();
    const { organization, loading: organizationLoading } =
        useOrganizationResource();
    // need to fetch sender from API to grab cvs vs hl7 format info
    const { sender, loading: senderLoading } = useSenderResource();

    const accessToken = oktaToken?.accessToken;
    const parsedName = memberships.state.active?.parsedName;
    const senderName = memberships.state.active?.senderName;
    const client = `${parsedName}.${senderName}`;

    const handleFileChange = async (
        event: React.ChangeEvent<HTMLInputElement>
    ) => {
        if (!event?.target?.files?.length) {
            // no files selected
            return;
        }
        const file = event.target.files.item(0);
        if (!file) {
            // shouldn't happen but keeps linter happy
            return;
        }
        // unfortunate that we have to do this a bit early in order to keep
        // async code out of the reducer, but oh well - DWS
        const content = await file.text();

        if (file.type === "csv" || file.type === "text/csv") {
            const localCsvError = parseCsvForError(file.name, content);
            if (localCsvError) {
                showError(localCsvError);
                return;
            }
        }

        setFileContent(content);

        dispatch({
            type: FileHandlerActionType.FILE_SELECTED,
            payload: { file },
        });
    };

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (fileContent.length === 0) {
            showError(`No File Contents To ${submitText}`);
            return;
        }

        // initializes necessary state and sets `isSubmitting`
        dispatch({ type: FileHandlerActionType.PREPARE_FOR_REQUEST });

        try {
            const response = await fetcher(
                client,
                fileName,
                contentType as string,
                fileContent,
                parsedName || "",
                accessToken || ""
            );
            // handles error and success cases via reducer
            dispatch({
                type: FileHandlerActionType.REQUEST_COMPLETE,
                payload: { response },
            });
        } catch (error) {
            // Noop.  Errors are collected below
            console.error("Unexpected error in file handler", error);
        }
    };

    const resetState = () => {
        setFileContent("");
        dispatch({ type: FileHandlerActionType.RESET });
    };

    const submitted = useMemo(
        () => !!(reportId || errors.length),
        [reportId, errors.length]
    );

    const successDescription = useMemo(() => {
        let suffix = "";
        if (handlerType === FileHandlerType.UPLOAD) {
            suffix = " and will be transmitted";
        }
        const schemaDescription =
            fileType === FileType.HL7
                ? "ReportStream standard HL7 v2.5.1"
                : "standard CSV";
        return `Your file meets the ${schemaDescription} schema${suffix}.`;
    }, [fileType, handlerType]);

    const warningDescription = useMemo(() => {
        return handlerType === FileHandlerType.UPLOAD
            ? "Your file has been transmitted"
            : "Your file has passed validation";
    }, [handlerType]);

    // default to FILE messaging here, partly to simplify typecheck
    const errorMessaging = useMemo(
        () => errorMessagingMap[handlerType][errorType || ErrorType.FILE],
        [errorType, handlerType]
    );

    const formLabel = useMemo(() => {
        if (!sender) {
            return "";
        }
        const fileTypeDescription =
            sender.format === "CSV" ? "a CSV" : "an HL7 v2.5.1";
        return `Select ${fileTypeDescription} formatted file to ${submitText.toLowerCase()}. Make sure that your file has a .${sender.format.toLowerCase()} extension.`;
    }, [sender, submitText]);

    if (senderLoading || organizationLoading) {
        return <FileHandlerSpinner message="Loading..." />;
    }

    if (!sender) {
        return (
            <div className="grid-container usa-section margin-bottom-10">
                <h1 className="margin-top-0 margin-bottom-5">{headingText}</h1>
                <h2 className="font-sans-lg">{organization?.description}</h2>
                <NoSenderBanner
                    action={handlerType}
                    organization={
                        organization?.description || "your organization"
                    }
                />
            </div>
        );
    }

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
                    message={`The following warnings were returned while processing your file. ${warningDescription}, but these warning areas can be addressed to enhance clarity.`}
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
                <FileHandlerSpinner message="Processing file..." />
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
