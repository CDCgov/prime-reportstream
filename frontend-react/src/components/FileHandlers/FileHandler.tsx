import React, { useEffect, useState } from "react";
import { GridContainer } from "@trussworks/react-uswds";

import { showError } from "../AlertNotifications";
import useFileHandler, {
    FileHandlerActionType,
    FileHandlerState,
} from "../../hooks/UseFileHandler";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import site from "../../content/site.json";
import { USExtLink, USLink } from "../USLink";
import { SchemaOption } from "../../senders/hooks/UseSenderSchemaOptions";
import { WatersResponse } from "../../config/endpoints/waters";
import Alert from "../../shared/Alert/Alert";

import FileHandlerFileUploadStep from "./FileHandlerFileUploadStep";
import FileHandlerSchemaSelectionStep from "./FileHandlerSchemaSelectionStep";
import FileHandlerErrorsWarningsStep from "./FileHandlerErrorsWarningsStep";
import FileHandlerSuccessStep from "./FileHandlerSuccessStep";

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
            showError(localError);
        }
    }, [localError]);

    const { data: organization } = useOrganizationSettings();

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

    function handleFileChange(file: File, fileContent: string) {
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

    function handleFileSubmitSuccess(response: WatersResponse) {
        dispatch({
            type: FileHandlerActionType.REQUEST_COMPLETE,
            payload: { response },
        });
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
            <article className="usa-section">
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
                                        {...commonStepProps}
                                        onFileChange={handleFileChange}
                                        onFileSubmitError={
                                            handleResetToFileSelection
                                        }
                                        onFileSubmitSuccess={
                                            handleFileSubmitSuccess
                                        }
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
                        <USLink href="/resources/api/documentation/data-model">
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
