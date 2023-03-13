import React, { ReactNode, useCallback, useEffect, useState } from "react";

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
import useSenderSchemaOptions, {
    SchemaOption,
} from "../../senders/hooks/UseSenderSchemaOptions";
import { useSenderResource } from "../../hooks/UseSenderResource";
import { RSSender } from "../../config/endpoints/settings";
import { MembershipSettings } from "../../hooks/UseOktaMemberships";
import site from "../../content/site.json";
import { USExtLink } from "../USLink";

import { FileHandlerStepTwo } from "./FileHandlerStepTwo";
import {
    RequestLevel,
    FileQualityFilterDisplay,
    FileSuccessDisplay,
    RequestedChangesDisplay,
} from "./FileHandlerMessaging";
import { FileHandlerStepOne } from "./FileHandlerStepOne";
import { FileHandlerStepThree } from "./FileHandlerStepThree";
import { FileHandlerStepFour } from "./FileHandlerStepFour";

export const FileHandlerSpinner = ({ message }: { message: ReactNode }) => (
    <div className="grid-col flex-1 display-flex flex-column flex-align-center margin-top-10">
        <div className="grid-row">
            <Spinner />
        </div>
        <div className="text-center">{message}</div>
    </div>
);

const SERVER_ERROR_MESSAGING = {
    heading: OverallStatus.ERROR,
    message: "There was a server error. Your file has not been accepted.",
};

const errorMessagingMap = {
    server: SERVER_ERROR_MESSAGING,
    file: {
        heading: "File did not pass validation",
        message: "Resubmit with the required edits.",
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

enum FileHandlerSteps {
    STEP_ONE = "stepOne",
    STEP_TWO = "stepTwo",
    STEP_THREE = "stepThree",
    STEP_FOUR = "stepFour",
}

const fileHandlerStepsArray = [
    FileHandlerSteps.STEP_ONE,
    FileHandlerSteps.STEP_TWO,
    FileHandlerSteps.STEP_THREE,
    FileHandlerSteps.STEP_FOUR,
];

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
    const [fileHandlerStepIndex, setFileHandlerStepIndex] = useState(0);
    const fileHandlerStep = fileHandlerStepsArray[fileHandlerStepIndex];

    const handlePrevFileHandlerStep = () => {
        setFileHandlerStepIndex(fileHandlerStepIndex - 1);
    };

    const handleNextFileHandlerStep = () => {
        setFileHandlerStepIndex(fileHandlerStepIndex + 1);
    };

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
        handleNextFileHandlerStep();
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
        return <FileHandlerSpinner message={<p>Loading...</p>} />;
    }

    const handleOnSchemaChange = (schemaOption: SchemaOption | null) =>
        dispatch({
            type: FileHandlerActionType.SCHEMA_SELECTED,
            payload: schemaOption,
        });

    return (
        <div className="grid-container usa-section margin-bottom-10">
            <h1 className="margin-top-0 margin-bottom-5">
                ReportStream File Validator
            </h1>
            <h2 className="font-sans-lg">{organization?.description}</h2>
            {(fileHandlerStep === FileHandlerSteps.STEP_ONE ||
                fileHandlerStep === FileHandlerSteps.STEP_TWO) && (
                <>
                    <p className="text-bold">
                        Check that public health departments can receive your
                        data through ReportStream by validating your file
                        format.{" "}
                    </p>
                    <p className="text-bold">
                        Reminder: Do not submit PII. Email{" "}
                        <USExtLink href={`mailto: ${site.orgs.RS.email}`}>
                            reportstream@cdc.gov
                        </USExtLink>
                        if you need fake data to use.
                    </p>
                </>
            )}
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

            {fileHandlerStep === FileHandlerSteps.STEP_ONE && (
                <FileHandlerStepOne
                    fileType={fileType}
                    schemaOptions={schemaOptions}
                    selectedSchemaOption={selectedSchemaOption}
                    onSchemaChange={handleOnSchemaChange}
                    handleNextFileHandlerStep={handleNextFileHandlerStep}
                />
            )}
            {fileHandlerStep === FileHandlerSteps.STEP_TWO && (
                <FileHandlerStepTwo
                    handleSubmit={handleSubmit}
                    handleFileChange={handleFileChange}
                    resetState={resetState}
                    fileInputResetValue={fileInputResetValue}
                    fileName={fileName}
                    submitted={submitted}
                    cancellable={cancellable}
                    formLabel={formLabel}
                    selectedSchemaOption={selectedSchemaOption}
                    isWorking={isWorking}
                    handlePrevFileHandlerStep={handlePrevFileHandlerStep}
                />
            )}
            {fileHandlerStep === FileHandlerSteps.STEP_THREE && (
                <FileHandlerStepThree
                    destinations={destinations}
                    errorMessaging={errorMessaging}
                    errors={errors}
                    hasQualityFilterMessages={hasQualityFilterMessages}
                    isFileSuccess={isFileSuccess}
                    qualityFilterMessages={qualityFilterMessages}
                    reportId={reportId}
                    successDescription={successDescription}
                    successTimestamp={successTimestamp}
                    warnings={warnings}
                    selectedSchemaOption={selectedSchemaOption}
                    handlePrevFileHandlerStep={handlePrevFileHandlerStep}
                    handleNextFileHandlerStep={handleNextFileHandlerStep}
                />
            )}
            {fileHandlerStep === FileHandlerSteps.STEP_FOUR && (
                <FileHandlerStepFour
                    destinations={destinations}
                    successTimestamp={successTimestamp}
                    reportId={reportId}
                />
            )}
            <p className="margin-top-10">
                Question or feedback? Please email{" "}
                <USExtLink href={`mailto: ${site.orgs.RS.email}`}>
                    reportstream@cdc.gov
                </USExtLink>
            </p>
        </div>
    );
}

export default FileHandler;
