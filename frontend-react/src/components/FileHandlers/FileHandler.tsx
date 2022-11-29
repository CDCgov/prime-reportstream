import React, { useCallback, useEffect, useMemo, useState } from "react";

import { showError } from "../AlertNotifications";
import { useSessionContext } from "../../contexts/SessionContext";
import { useSenderResource } from "../../hooks/UseSenderResource";
import { OverallStatus, WatersResponse } from "../../config/endpoints/waters";
import Spinner from "../Spinner"; // TODO: refactor to use suspense
import useFileHandler, {
    ErrorType,
    FileHandlerActionType,
    FileType,
} from "../../hooks/UseFileHandler";
import { parseCsvForError } from "../../utils/FileUtils";
import { useWatersUploader } from "../../hooks/network/WatersHooks";
import { NoServicesBanner } from "../alerts/NoServicesAlert";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";

import {
    RequestLevel,
    FileQualityFilterDisplay,
    FileSuccessDisplay,
    FileWarningBanner,
    RequestedChangesDisplay,
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
    heading: OverallStatus.ERROR,
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
        overallStatus,
        reportItems,
    } = state;

    useEffect(() => {
        if (localError) {
            showError(localError);
        }
    }, [localError]);

    const { activeMembership } = useSessionContext();
    // TODO: Transition from isLoading to Suspense component
    const { data: organization, isLoading: organizationLoading } =
        useOrganizationSettings();
    // need to fetch sender from API to grab cvs vs hl7 format info
    const { senderDetail: sender, senderIsLoading } = useSenderResource();

    const parsedName = activeMembership?.parsedName;
    const senderName = activeMembership?.service;
    const client = `${parsedName}.${senderName}`;
    const validateOnly = useMemo(
        () => handlerType !== FileHandlerType.UPLOAD,
        [handlerType]
    );
    const uploaderCallback = useCallback(
        (data?: WatersResponse) => {
            dispatch({
                type: FileHandlerActionType.REQUEST_COMPLETE,
                payload: { response: data!! }, // Strong asserting that this won't be undefined
            });
        },
        [dispatch]
    );
    const { sendFile, isWorking } = useWatersUploader(
        uploaderCallback,
        validateOnly
    );

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
        await sendFile({
            contentType: contentType,
            fileContent: fileContent,
            fileName: fileName,
            client: client,
        });
    };

    const resetState = () => {
        setFileContent("");
        dispatch({ type: FileHandlerActionType.RESET });
    };

    const submitted = useMemo(
        () => !!(reportId || errors.length || overallStatus),
        [reportId, errors.length, overallStatus]
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
        return `The file meets the ${schemaDescription} schema${suffix}.`;
    }, [fileType, handlerType]);

    const warningHeading = useMemo(() => {
        return handlerType === FileHandlerType.VALIDATION
            ? `${successMessage} with recommended edits`
            : "";
    }, [handlerType, successMessage]);

    const warningDescription = useMemo(() => {
        return handlerType === FileHandlerType.UPLOAD
            ? "Your file has been transmitted, but these warning areas can be addressed to enhance clarity."
            : "The following warnings were returned while processing your file. We recommend addressing warnings to enhance clarity.";
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

    // Array containing only qualityFilterMessages that have filteredReportItems.
    const qualityFilterMessages = useMemo(
        () => reportItems?.filter((d) => d.filteredReportItems.length > 0),
        [reportItems]
    );

    const hasQualityFilterMessages =
        destinations.length > 0 &&
        qualityFilterMessages &&
        qualityFilterMessages.length > 0;

    const isFileSuccess =
        (reportId || overallStatus === OverallStatus.VALID) &&
        !hasQualityFilterMessages;

    if (senderIsLoading || organizationLoading) {
        return <FileHandlerSpinner message="Loading..." />;
    }

    if (!sender) {
        return (
            <div className="grid-container usa-section margin-bottom-10">
                <h1 className="margin-top-0 margin-bottom-5">{headingText}</h1>
                <h2 className="font-sans-lg">{organization?.description}</h2>
                <NoServicesBanner
                    featureName={handlerType}
                    organization={organization?.description}
                    serviceType={"sender"}
                />
            </div>
        );
    }

    return (
        <div className="grid-container usa-section margin-bottom-10">
            <h1 className="margin-top-0 margin-bottom-5">{headingText}</h1>
            <h2 className="font-sans-lg">{organization?.description}</h2>
            {fileName && (
                <>
                    <p
                        id="validatedFilename"
                        className="text-normal text-base margin-bottom-0"
                    >
                        File name
                    </p>
                    <p className="margin-top-05">{fileName}</p>
                </>
            )}
            {showWarningBanner && (
                <FileWarningBanner message={warningText || ""} />
            )}
            {isFileSuccess && warnings.length === 0 && (
                <FileSuccessDisplay
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
            {warnings.length > 0 && (
                <RequestedChangesDisplay
                    title={RequestLevel.WARNING}
                    data={warnings}
                    message={warningDescription}
                    heading={warningHeading}
                    handlerType={handlerType}
                />
            )}
            {errors.length > 0 && (
                <RequestedChangesDisplay
                    title={RequestLevel.ERROR}
                    data={errors}
                    message={errorMessaging.message}
                    heading={errorMessaging.heading}
                    handlerType={handlerType}
                />
            )}
            {hasQualityFilterMessages && (
                <FileQualityFilterDisplay
                    destinations={qualityFilterMessages}
                    heading=""
                    message={`The file does not meet the jurisdiction's schema. Please resolve the errors below.`}
                />
            )}
            {isWorking && <FileHandlerSpinner message="Processing file..." />}
            {!isWorking && (
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
