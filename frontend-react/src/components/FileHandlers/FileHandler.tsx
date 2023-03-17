import React, { useCallback, useEffect, useState } from "react";
import { GridContainer } from "@trussworks/react-uswds";

import { showError } from "../AlertNotifications";
import { useSessionContext } from "../../contexts/SessionContext";
import { OverallStatus, WatersResponse } from "../../config/endpoints/waters";
import Spinner from "../Spinner"; // TODO: refactor to use suspense
import useFileHandler, {
    ErrorType,
    FileHandlerActionType,
    FileType,
} from "../../hooks/UseFileHandler";
import { parseCsvForError } from "../../utils/FileUtils";
import { useWatersUploader } from "../../hooks/network/WatersHooks";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import { EventName, trackAppInsightEvent } from "../../utils/Analytics";
import useSenderSchemaOptions from "../../senders/hooks/UseSenderSchemaOptions";
import { useSenderResource } from "../../hooks/UseSenderResource";
import { RSSender } from "../../config/endpoints/settings";
import { MembershipSettings } from "../../hooks/UseOktaMemberships";

import { FileHandlerForm } from "./FileHandlerForm";
import {
    RequestLevel,
    FileQualityFilterDisplay,
    FileSuccessDisplay,
    RequestedChangesDisplay,
} from "./FileHandlerMessaging";

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
    server: SERVER_ERROR_MESSAGING,
    file: {
        heading: "Your file has not passed validation",
        message: "Please review the errors below.",
    },
};

export const SUCCESS_DESCRIPTIONS = {
    [FileType.CSV]: "The file meets the standard CSV schema.",
    [FileType.HL7]:
        "The file meets the ReportStream standard HL7 v2.5.1 schema.",
};

export const UPLOAD_PROMPT_DESCRIPTIONS = {
    [FileType.CSV]:
        "Select a CSV formatted file to validate. Make sure that your file has a .csv extension.",
    [FileType.HL7]:
        "Select an HL7 v2.5.1 formatted file to validate. Make sure that your file has a .hl7 extension.",
};

/**
 * Given a user's membership settings and their Sender details,
 * return the client string to send to the validate endpoint
 *
 * Only send the client when the selected schema matches the Sender's schema --
 * this is to account for factoring in Sender settings into the validation
 * e.g., allowDuplicates in https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/src/main/kotlin/azure/ValidateFunction.kt#L100
 *
 * @param selectedSchemaName { string | undefined }
 * @param activeMembership { MembershipSettings | undefined}
 * @param sender { RSSender | undefined }
 * @returns {string} The value sent as the client header (can be a blank string)
 */
export function getClientHeader(
    selectedSchemaName: string | undefined,
    activeMembership: MembershipSettings | null | undefined,
    sender: RSSender | undefined
) {
    const parsedName = activeMembership?.parsedName;
    const senderName = activeMembership?.service;

    if (parsedName && senderName && sender?.schemaName === selectedSchemaName) {
        return `${parsedName}.${senderName}`;
    }

    return "";
}

function FileHandler() {
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
        selectedSchemaOption,
    } = state;

    useEffect(() => {
        if (localError) {
            showError(localError);
        }
    }, [localError]);

    const { activeMembership } = useSessionContext();
    const { senderDetail } = useSenderResource();
    // TODO: Transition from isLoading to Suspense component
    const { data: organization } = useOrganizationSettings();
    const { schemaOptions, isLoading: isSenderSchemaOptionsLoading } =
        useSenderSchemaOptions();

    const uploaderCallback = useCallback(
        (data?: WatersResponse) => {
            dispatch({
                type: FileHandlerActionType.REQUEST_COMPLETE,
                payload: { response: data!! }, // Strong asserting that this won't be undefined
            });
        },
        [dispatch]
    );
    const { sendFile, isWorking } = useWatersUploader(uploaderCallback);

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
            showError("No file contents to validate");
            return;
        }

        if (!selectedSchemaOption) {
            showError("No schema selected");
            return;
        }

        // initializes necessary state and sets `isSubmitting`
        dispatch({ type: FileHandlerActionType.PREPARE_FOR_REQUEST });

        let eventData;

        try {
            const response = await sendFile({
                contentType: contentType,
                fileContent: fileContent,
                fileName: fileName,
                client: getClientHeader(
                    selectedSchemaOption.value,
                    activeMembership,
                    senderDetail
                ),
                schema: selectedSchemaOption.value,
                format: selectedSchemaOption.format,
            });

            eventData = {
                warningCount: response?.warnings?.length,
                errorCount: response?.errors?.length,
            };
        } catch (e: any) {
            if (e.data) {
                eventData = {
                    warningCount: e.data.warningCount,
                    errorCount: e.data.errorCount,
                };
            }
        }

        if (eventData) {
            trackAppInsightEvent(EventName.FILE_VALIDATOR, {
                fileValidator: {
                    schema: selectedSchemaOption?.value,
                    fileType: fileType,
                    sender: organization?.name,
                    ...eventData,
                },
            });
        }
    };

    const resetState = () => {
        setFileContent("");
        dispatch({ type: FileHandlerActionType.RESET });
    };

    const submitted = !!(reportId || errors.length || overallStatus);

    // default to FILE messaging here, partly to simplify typecheck
    const errorMessaging = errorMessagingMap[errorType || ErrorType.FILE];

    let formLabel = "";
    let successDescription = "";
    if (selectedSchemaOption) {
        formLabel = UPLOAD_PROMPT_DESCRIPTIONS[selectedSchemaOption.format];
        successDescription = SUCCESS_DESCRIPTIONS[selectedSchemaOption.format];
    }

    // Array containing only qualityFilterMessages that have filteredReportItems.
    const qualityFilterMessages = reportItems?.filter(
        (d) => d.filteredReportItems.length > 0
    );

    const hasQualityFilterMessages =
        destinations.length > 0 &&
        qualityFilterMessages &&
        qualityFilterMessages.length > 0;

    const isFileSuccess =
        (reportId || overallStatus === OverallStatus.VALID) &&
        !hasQualityFilterMessages;

    if (isSenderSchemaOptionsLoading) {
        return <FileHandlerSpinner message="Loading..." />;
    }

    return (
        <GridContainer>
            <article className="usa-section">
                <h1 className="margin-top-0 margin-bottom-5">
                    ReportStream File Validator
                </h1>
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
                {isFileSuccess && warnings.length === 0 && (
                    <FileSuccessDisplay
                        extendedMetadata={{
                            destinations,
                            timestamp: successTimestamp,
                            reportId,
                        }}
                        heading="Validate another file"
                        message={successDescription}
                        showExtendedMetadata={false}
                    />
                )}
                {warnings.length > 0 && (
                    <RequestedChangesDisplay
                        title={RequestLevel.WARNING}
                        data={warnings}
                        message="The following warnings were returned while processing your file. We recommend addressing warnings to enhance clarity."
                        heading="File validated with recommended edits"
                    />
                )}
                {errors.length > 0 && (
                    <RequestedChangesDisplay
                        title={RequestLevel.ERROR}
                        data={errors}
                        message={errorMessaging.message}
                        heading={errorMessaging.heading}
                    />
                )}
                {hasQualityFilterMessages && (
                    <FileQualityFilterDisplay
                        destinations={qualityFilterMessages}
                        heading=""
                        message={`The file does not meet the jurisdiction's schema. Please resolve the errors below.`}
                    />
                )}
                {isWorking && (
                    <FileHandlerSpinner message="Processing file..." />
                )}
                {!isWorking && (
                    <FileHandlerForm
                        handleSubmit={handleSubmit}
                        handleFileChange={handleFileChange}
                        resetState={resetState}
                        fileInputResetValue={fileInputResetValue}
                        submitted={submitted}
                        cancellable={cancellable}
                        fileName={fileName}
                        fileType={fileType}
                        formLabel={formLabel}
                        resetText="Validate another file"
                        submitText="Validate"
                        schemaOptions={schemaOptions}
                        selectedSchemaOption={selectedSchemaOption}
                        onSchemaChange={(schemaOption) =>
                            dispatch({
                                type: FileHandlerActionType.SCHEMA_SELECTED,
                                payload: schemaOption,
                            })
                        }
                    />
                )}
            </article>
        </GridContainer>
    );
}

export default FileHandler;
