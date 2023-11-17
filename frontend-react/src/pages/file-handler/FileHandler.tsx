import { useEffect, useState } from "react";
import { GridContainer } from "@trussworks/react-uswds";

import useFileHandler, {
    FileHandlerActionType,
    FileHandlerState,
} from "../../hooks/UseFileHandler";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import site from "../../content/site.json";
import { USExtLink, USLink } from "../../components/USLink";
import { SchemaOption } from "../../senders/hooks/UseSenderSchemaOptions";
import Alert from "../../shared/Alert/Alert";
import { EventName, useAppInsightsContext } from "../../contexts/AppInsights";
import { useSessionContext } from "../../contexts/Session";
import useSenderResource from "../../hooks/UseSenderResource";
import { useWatersUploader } from "../../hooks/network/WatersHooks";
import { useToast } from "../../contexts/Toast";
import FileHandlerFileUploadStep, {
    getClientHeader,
} from "../../components/FileHandlers/FileHandlerFileUploadStep";
import FileHandlerSchemaSelectionStep from "../../components/FileHandlers/FileHandlerSchemaSelectionStep";
import FileHandlerErrorsWarningsStep from "../../components/FileHandlers/FileHandlerErrorsWarningsStep";
import FileHandlerSuccessStep from "../../components/FileHandlers/FileHandlerSuccessStep";
import { parseCsvForError } from "../../utils/FileUtils";

export interface FileHandlerStepProps extends FileHandlerState {
    isValid?: boolean;
    shouldSkip?: boolean;
    onPrevStepClick: () => void;
    onNextStepClick: () => void;
}

function mapStateToOrderedSteps(state: FileHandlerState) {
    const { selectedSchemaOption, file, errors, warnings, overallStatus } =
        state;

    return [
        {
            Component: FileHandlerSchemaSelectionStep,
            isValid: Boolean(selectedSchemaOption.value),
        },
        {
            Component: FileHandlerFileUploadStep,
            isValid: Boolean(file),
        },
        {
            Component: FileHandlerErrorsWarningsStep,
            isValid: false,
            shouldSkip: Boolean(
                overallStatus && errors.length === 0 && warnings.length === 0,
            ),
        },
        {
            Component: FileHandlerSuccessStep,
            isValid: true,
        },
    ];
}

export default function FileHandler() {
    const { toast } = useToast();
    const { appInsights } = useAppInsightsContext();
    const { data: organization } = useOrganizationSettings();
    const { data: sender } = useSenderResource();
    const { activeMembership } = useSessionContext();
    const { mutateAsync: sendFile, isPending: isSubmitting } =
        useWatersUploader();
    const { state, dispatch } = useFileHandler();
    const { fileName, localError } = state;
    const orderedSteps = mapStateToOrderedSteps(state).filter(
        (step) => !step.shouldSkip,
    );
    const [currentStepIndex, setCurrentStepIndex] = useState(0);
    const {
        Component: StepComponent,
        isValid,
        shouldSkip,
    } = orderedSteps[currentStepIndex];

    useEffect(() => {
        if (localError) {
            toast(localError, "error");
        }
    }, [localError, toast]);

    function decrementStepIndex() {
        if (currentStepIndex === 0) {
            return;
        }

        setCurrentStepIndex((idx) => idx - 1);
    }

    function incrementStepIndex() {
        if (currentStepIndex >= orderedSteps.length - 1) {
            return;
        }

        setCurrentStepIndex((idx) => idx + 1);
    }

    function handleSchemaChange(schemaOption: SchemaOption) {
        dispatch({
            type: FileHandlerActionType.SCHEMA_SELECTED,
            payload: schemaOption,
        });
    }

    async function handleFileChange(ev: React.ChangeEvent<HTMLInputElement>) {
        // TODO: consolidate with upcoming FileUtils generic function
        if (!ev?.target?.files?.length) {
            return;
        }
        const file = ev.target.files.item(0)!!;

        const fileContent = await file.text();

        if (file.type === "csv" || file.type === "text/csv") {
            const localCsvError = parseCsvForError(file.name, fileContent);
            if (localCsvError) {
                toast(localCsvError, "error");
                return;
            }
        }
        dispatch({
            type: FileHandlerActionType.FILE_SELECTED,
            payload: { file, fileContent },
        });
    }

    function handleResetToFileSelection() {
        dispatch({
            type: FileHandlerActionType.RESET,
            payload: {
                selectedSchemaOption: state.selectedSchemaOption,
            },
        });

        const fileSelectionStepIndex = orderedSteps.findIndex(
            ({ Component }) => Component === FileHandlerFileUploadStep,
        );
        setCurrentStepIndex(fileSelectionStepIndex);
        window.scrollTo(0, 0);
    }

    async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();

        const {
            fileContent,
            contentType,
            file,
            selectedSchemaOption,
            fileType,
        } = state;

        if (fileContent.length === 0) {
            toast("No file contents to validate", "error");
            return;
        }

        let eventData;
        try {
            const response = await sendFile({
                contentType,
                fileContent,
                fileName: file?.name!!,
                client: getClientHeader(
                    selectedSchemaOption.value,
                    activeMembership,
                    sender,
                ),
                schema: selectedSchemaOption.value,
                format: selectedSchemaOption.format,
            });

            dispatch({
                type: FileHandlerActionType.REQUEST_COMPLETE,
                payload: { response },
            });

            incrementStepIndex();

            eventData = {
                warningCount: response?.warnings?.length,
                errorCount: response?.errors?.length,
                overallStatus: response?.overallStatus,
            };
        } catch (e: any) {
            // TODO: update this when we're still sending 200s back on validation warnings/errors
            if (e.data) {
                eventData = {
                    warningCount: e.data.warningCount,
                    errorCount: e.data.errorCount,
                };
            }

            toast("File validation error. Please try again.", "error");

            handleResetToFileSelection();
        }

        if (eventData) {
            appInsights?.trackEvent({
                name: EventName.FILE_VALIDATOR,
                properties: {
                    fileValidator: {
                        schema: selectedSchemaOption?.value,
                        fileType: fileType,
                        sender: organization?.name,
                        ...eventData,
                    },
                },
            });
        }
    }

    const commonStepProps = {
        ...state,
        isValid: isValid,
        shouldSkip: shouldSkip,
        onPrevStepClick: decrementStepIndex,
        onNextStepClick: incrementStepIndex,
    };

    return (
        <GridContainer>
            <article>
                <h1 className="margin-y-4">ReportStream File Validator</h1>

                {organization?.description && (
                    <h2 className="font-sans-lg">{organization.description}</h2>
                )}

                {fileName && (
                    <div className="margin-bottom-3">
                        <p className="margin-bottom-1 text-normal text-base">
                            File name
                        </p>
                        <p className="margin-top-0">{fileName}</p>
                    </div>
                )}

                <div className="margin-bottom-4">
                    {(() => {
                        // The File Validate tool now has 4 discrete steps,
                        // Schema Select, File Select, [optional]Show Errors, Success Page
                        // The stages can be seen here: https://figma.fun/fGCeo4
                        //
                        // TODO: generalize the reducer state so we can just render <StepComponent>
                        switch (StepComponent) {
                            case FileHandlerSchemaSelectionStep:
                                return (
                                    <FileHandlerSchemaSelectionStep
                                        {...commonStepProps}
                                        onSchemaChange={handleSchemaChange}
                                    />
                                );
                            case FileHandlerFileUploadStep:
                                return (
                                    <FileHandlerFileUploadStep
                                        onSubmit={handleSubmit}
                                        isSubmitting={isSubmitting}
                                        onFileChange={handleFileChange}
                                        onBack={decrementStepIndex}
                                        selectedSchemaOption={
                                            state.selectedSchemaOption
                                        }
                                        isValid={isValid}
                                    />
                                );
                            case FileHandlerErrorsWarningsStep:
                                return (
                                    <FileHandlerErrorsWarningsStep
                                        {...commonStepProps}
                                        onTestAnotherFileClick={
                                            handleResetToFileSelection
                                        }
                                    />
                                );
                            case FileHandlerSuccessStep:
                                return <FileHandlerSuccessStep />;
                            default:
                                return null;
                        }
                    })()}
                </div>
                {StepComponent !== FileHandlerSuccessStep && (
                    <Alert headingLevel="h3" type="tip">
                        Reference{" "}
                        <USLink href="/developer-resources/api/documentation/data-model">
                            the data model
                        </USLink>{" "}
                        for the information needed to validate your file
                        successfully. Pay special attention to which fields are
                        required and common mistakes.
                    </Alert>
                )}
                <p className="text-base-darker margin-top-10">
                    Questions or feedback? Please email{" "}
                    <USExtLink href={`mailto: ${site.orgs.RS.email}`}>
                        {site.orgs.RS.email}
                    </USExtLink>
                </p>
            </article>
        </GridContainer>
    );
}
