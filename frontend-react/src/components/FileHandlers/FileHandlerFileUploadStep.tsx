import React, { useRef } from "react";
import {
    Button,
    Form,
    FormGroup,
    Label,
    FileInput,
    FileInputRef,
} from "@trussworks/react-uswds";

import { parseCsvForError } from "../../utils/FileUtils";
import { useWatersUploader } from "../../hooks/network/WatersHooks";
import { EventName, trackAppInsightEvent } from "../../utils/Analytics";
import { showError } from "../AlertNotifications";
import { RSSender } from "../../config/endpoints/settings";
import { MembershipSettings } from "../../hooks/UseOktaMemberships";
import useSenderResource from "../../hooks/UseSenderResource";
import Spinner from "../Spinner";
import { useSessionContext } from "../../contexts/SessionContext";
import { WatersResponse } from "../../config/endpoints/waters";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import { FileType } from "../../utils/TemporarySettingsAPITypes";

import FileHandlerPiiWarning from "./FileHandlerPiiWarning";
import { FileHandlerStepProps } from "./FileHandler";

export const UPLOAD_PROMPT_DESCRIPTIONS = {
    [FileType.CSV]: {
        title: "Upload CSV file",
        subtitle: "Make sure your file has a .csv extension",
    },
    [FileType.HL7]: {
        title: "Upload HL7 v2.5.1 file",
        subtitle: "Make sure your file has a .hl7 extension",
    },
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
    sender: RSSender | undefined,
) {
    const parsedName = activeMembership?.parsedName;
    const senderName = activeMembership?.service;

    if (parsedName && senderName && sender?.schemaName === selectedSchemaName) {
        return `${parsedName}.${senderName}`;
    }

    return "";
}

export interface FileHandlerFileUploadStepProps extends FileHandlerStepProps {
    onFileChange: (file: File, fileContent: string) => void;
    onFileSubmitError: () => void;
    onFileSubmitSuccess: (response: WatersResponse) => void;
}

const BASE_ACCEPT_VALUE = [".csv", ".hl7"].join(",");

export default function FileHandlerFileUploadStep({
    contentType,
    file,
    fileContent,
    fileType,
    isValid,
    onFileChange,
    onFileSubmitError,
    onFileSubmitSuccess,
    onNextStepClick,
    onPrevStepClick,
    selectedSchemaOption,
}: FileHandlerFileUploadStepProps) {
    const { data: organization } = useOrganizationSettings();
    const {
        data: senderDetail,
        isLoading: senderIsLoading,
        isInitialLoading: senderIsInitialLoading,
    } = useSenderResource();
    const { activeMembership } = useSessionContext();
    const fileInputRef = useRef<FileInputRef>(null);
    const { format } = selectedSchemaOption;
    const accept = selectedSchemaOption
        ? `.${format.toLowerCase()}`
        : BASE_ACCEPT_VALUE;

    const { sendFile, isWorking: isUploading } = useWatersUploader();

    async function handleFileChange(
        event: React.ChangeEvent<HTMLInputElement>,
    ) {
        // TODO: consolidate with upcoming FileUtils generic function
        if (!event?.target?.files?.length) {
            onFileSubmitError();
            return;
        }
        const selectedFile = event.target.files.item(0)!!;

        const selectedFileContent = await selectedFile.text();

        if (selectedFile.type === "csv" || selectedFile.type === "text/csv") {
            const localCsvError = parseCsvForError(
                selectedFile.name,
                selectedFileContent,
            );
            if (localCsvError) {
                showError(localCsvError);
                return;
            }
        }

        onFileChange(selectedFile, selectedFileContent);
    }

    async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (fileContent.length === 0) {
            showError("No file contents to validate");
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
                    senderDetail,
                ),
                schema: selectedSchemaOption.value,
                format: selectedSchemaOption.format,
            });

            onFileSubmitSuccess(response);

            if (onNextStepClick) {
                onNextStepClick();
            }

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

            showError("File validation error. Please try again.");

            onFileSubmitError();
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
    }

    return (
        <div>
            <FileHandlerPiiWarning />

            {(() => {
                if (senderIsLoading && senderIsInitialLoading) {
                    return <Spinner />;
                }

                if (isUploading) {
                    return (
                        <div className="padding-y-4 text-center">
                            {/* HACK: need to remove grid-container from Spinner */}
                            <div className="grid-row flex-justify-center">
                                <Spinner />
                            </div>
                            <div className="grid-row">
                                <div className="margin-x-auto tablet:grid-col-5 line-height-sans-6">
                                    <p>
                                        Checking your file for any errors that
                                        will prevent your data from being
                                        reported successfully...
                                    </p>
                                </div>
                            </div>
                        </div>
                    );
                }

                const prompt = UPLOAD_PROMPT_DESCRIPTIONS[format];

                return (
                    <Form
                        name="fileValidation"
                        onSubmit={handleSubmit}
                        className="rs-full-width-form"
                    >
                        <FormGroup className="margin-top-0">
                            <Label
                                className="font-sans-xs"
                                id="upload-csv-input-label"
                                htmlFor="upload-csv-input"
                            >
                                <span className="display-block">
                                    {prompt.title}
                                </span>
                                <span className="display-block text-base">
                                    {prompt.subtitle}
                                </span>
                            </Label>
                            <FileInput
                                id="upload-csv-input"
                                name="upload-csv-input"
                                aria-describedby="upload-csv-input-label"
                                data-testid="upload-csv-input"
                                onChange={handleFileChange}
                                required
                                ref={fileInputRef}
                                accept={accept}
                            />
                        </FormGroup>

                        <div className="margin-top-05 margin-bottom-0 display-flex">
                            <Button
                                className="usa-button usa-button--outline"
                                type={"button"}
                                onClick={onPrevStepClick}
                            >
                                Back
                            </Button>
                            <Button
                                disabled={!isValid}
                                className="usa-button"
                                type="submit"
                            >
                                Submit
                            </Button>
                        </div>
                    </Form>
                );
            })()}
        </div>
    );
}
