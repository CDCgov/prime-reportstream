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
import { OverallStatus, WatersResponse } from "../../config/endpoints/waters";
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

const WizardSteps = [
    {
        order: 0,
        Component: FileHandlerSchemaSelectionStep,
        isValid(state: FileHandlerState) {
            return Boolean(state.selectedSchemaOption.value);
        },
    },
    {
        order: 1,
        Component: FileHandlerFileUploadStep,
        isValid(state: FileHandlerState) {
            return Boolean(state.file);
        },
    },
    {
        order: 2,
        Component: FileHandlerErrorsWarningsStep,
        isVisible(state: FileHandlerState) {
            return Boolean(state.errors?.length || state.warnings?.length);
        },
    },
    {
        order: 3,
        Component: FileHandlerSuccessStep,
    },
] satisfies {
    order: number;
    Component: React.ComponentType<any>;
    isValid?: (state: any) => boolean;
    isVisible?: (state: any) => boolean;
}[];

function getWizardStepNumber(state: FileHandlerState) {
    if (
        Boolean(state.overallStatus === OverallStatus.VALID) &&
        !Boolean(state.errors?.length || state.warnings?.length)
    ) {
        return 3;
    } else if (Boolean(state.errors?.length || state.warnings?.length)) {
        return 2;
    } else if (Boolean(state.selectedSchemaOption.value)) {
        return 1;
    }
    return 0;
}

function getWizardStep(
    state: FileHandlerState,
    n?: number,
    isForward: boolean = true,
) {
    let stepNum = n ?? getWizardStepNumber(state);
    let step;
    while (step == null) {
        const s = WizardSteps[stepNum];
        if (s.isVisible == null || s.isVisible?.(state)) {
            step = s;
        } else {
            if (isForward && stepNum < WizardSteps.length - 1) {
                stepNum++;
            } else if (stepNum > 0) {
                stepNum--;
            }
            throw new Error("Unable to determine next visible step");
        }
    }
    return {
        ...step,
        isValid: step.isValid == null ? true : step.isValid(state),
        isVisible: step.isVisible == null ? true : step.isVisible(state),
    } satisfies {
        order: number;
        Component: React.ComponentType<any>;
        isValid: boolean;
        isVisible: boolean;
    };
}

export default function FileHandler() {
    const { state, dispatch } = useFileHandler();
    const { fileName, localError } = state;
    const [currentStepIndex, setCurrentStepIndex] = useState(
        getWizardStep(state).order,
    );
    const { Component, isValid, isVisible } = getWizardStep(
        state,
        currentStepIndex,
    );

    useEffect(() => {
        if (localError) {
            showError(localError);
        }
    }, [localError]);

    const { data: organization } = useOrganizationSettings();

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

        setCurrentStepIndex(1);

        window.scrollTo(0, 0);
    }

    function handleFileSubmitSuccess(response: WatersResponse) {
        dispatch({
            type: FileHandlerActionType.REQUEST_COMPLETE,
            payload: { response },
        });
    }

    function decrementStepIndex() {
        setCurrentStepIndex(
            (idx) => getWizardStep(state, idx - 1, false).order,
        );
    }

    function incrementStepIndex() {
        setCurrentStepIndex((idx) => getWizardStep(state, idx + 1).order);
    }

    const commonStepProps = {
        ...state,
        isValid: isValid,
        shouldSkip: isVisible,
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
                        switch (Component) {
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
                {Component !== FileHandlerSuccessStep && (
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
