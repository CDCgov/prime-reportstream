import React, { ReactNode, useEffect, useState } from "react";

import { showError } from "../AlertNotifications";
import { useSessionContext } from "../../contexts/SessionContext";
import { OverallStatus } from "../../config/endpoints/waters";
import Spinner from "../Spinner"; // TODO: refactor to use suspense
import useFileHandler, {
    ErrorType,
    FileHandlerActionType,
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
import { FileType } from "../../utils/TemporarySettingsAPITypes";

import { FileHandlerStepTwo } from "./FileHandlerStepTwo";
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
    const {
        fileInputResetValue,
        contentType,
        fileType,
        fileName,
        errors,
        errorType,
        destinations,
        reportId,
        successTimestamp,
        warnings,
        localError,
        reportItems,
        selectedSchemaOption,
    } = state;
    const [fileContent, setFileContent] = useState("");
    const [fileHandlerStepIndex, setFileHandlerStepIndex] = useState(0);
    // The File Validate tool now has 4 discrete steps,
    // Schema Select, File Select, [optional]Show Errors, Success Page
    // The stages can be seen here: https://figma.fun/fGCeo4
    const fileHandlerStep = fileHandlerStepsArray[fileHandlerStepIndex];
    const resetState = () => {
        setFileContent("");
        dispatch({ type: FileHandlerActionType.RESET });
    };

    const handlePrevFileHandlerStep = () => {
        // We have to clear different parts of state depending
        // on what step they're coming from. From 3 -> 2, we
        // need to reset everything EXCEPT the selected schema
        // since they're just re-selecting a file, otherwise,
        // we can reset all of state.
        if (fileHandlerStep === FileHandlerSteps.STEP_TWO) {
            resetState();
            setFileHandlerStepIndex(fileHandlerStepIndex - 1);
        } else if (fileHandlerStep === FileHandlerSteps.STEP_THREE) {
            resetState();
            dispatch({
                type: FileHandlerActionType.SCHEMA_SELECTED,
                payload: selectedSchemaOption,
            });
            setFileHandlerStepIndex(fileHandlerStepIndex - 1);
        }
    };

    const handleNextFileHandlerStep = (nextStep = fileHandlerStepIndex + 1) => {
        setFileHandlerStepIndex(nextStep);
    };

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

    const { sendFile, isWorking } = useWatersUploader();

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
            dispatch({
                type: FileHandlerActionType.REQUEST_COMPLETE,
                payload: { response: response!! }, // Strong asserting that this won't be undefined
            });
            eventData = {
                warningCount: response?.warnings?.length,
                errorCount: response?.errors?.length,
                overallStatus: response?.overallStatus,
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
        const isFileSuccess =
            eventData?.overallStatus === OverallStatus.VALID &&
            !(eventData?.errorCount || eventData?.warningCount);
        if (isFileSuccess) {
            handleNextFileHandlerStep(3);
        } else {
            handleNextFileHandlerStep();
        }
    };

    // Array containing only qualityFilterMessages that have filteredReportItems.
    const qualityFilterMessages = reportItems?.filter(
        (d) => d.filteredReportItems.length > 0
    );

    // default to FILE messaging here, partly to simplify typecheck
    const errorMessaging = errorMessagingMap[errorType || ErrorType.FILE];

    const hasQualityFilterMessages =
        destinations.length > 0 &&
        qualityFilterMessages &&
        qualityFilterMessages.length > 0;

    if (isSenderSchemaOptionsLoading) {
        return <FileHandlerSpinner message={<p>Loading...</p>} />;
    }

    const handleOnSchemaChange = (schemaOption: SchemaOption) => {
        dispatch({
            type: FileHandlerActionType.SCHEMA_SELECTED,
            payload: schemaOption,
        });
    };

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
                            {site.orgs.RS.email}
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
                    handleNextFileHandlerStep={handleNextFileHandlerStep}
                    onSchemaChange={handleOnSchemaChange}
                    schemaOptions={schemaOptions}
                    selectedSchemaOption={selectedSchemaOption}
                />
            )}
            {fileHandlerStep === FileHandlerSteps.STEP_TWO && (
                <FileHandlerStepTwo
                    handleSubmit={handleSubmit}
                    handleFileChange={handleFileChange}
                    fileInputResetValue={fileInputResetValue}
                    fileName={fileName}
                    selectedSchemaOption={selectedSchemaOption}
                    isWorking={isWorking}
                    handlePrevFileHandlerStep={handlePrevFileHandlerStep}
                />
            )}
            {fileHandlerStep === FileHandlerSteps.STEP_THREE && (
                <FileHandlerStepThree
                    errors={errors}
                    errorMessaging={errorMessaging}
                    handleNextFileHandlerStep={handleNextFileHandlerStep}
                    handlePrevFileHandlerStep={handlePrevFileHandlerStep}
                    hasQualityFilterMessages={hasQualityFilterMessages}
                    qualityFilterMessages={qualityFilterMessages}
                    selectedSchemaOption={selectedSchemaOption}
                    warnings={warnings}
                />
            )}
            {fileHandlerStep === FileHandlerSteps.STEP_FOUR && (
                <FileHandlerStepFour
                    destinations={destinations}
                    reportId={reportId}
                    successTimestamp={successTimestamp}
                />
            )}
            <p className="margin-top-10">
                Question or feedback? Please email{" "}
                <USExtLink href={`mailto: ${site.orgs.RS.email}`}>
                    {site.orgs.RS.email}
                </USExtLink>
            </p>
        </div>
    );
}

export default FileHandler;
