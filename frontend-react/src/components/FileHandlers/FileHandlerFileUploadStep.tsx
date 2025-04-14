import { Button, FileInput, FileInputRef, Form, FormGroup, Label } from "@trussworks/react-uswds";
import { ChangeEvent, FormEvent, useRef } from "react";

import { FileHandlerStepProps } from "./FileHandler";
import FileHandlerPiiWarning from "./FileHandlerPiiWarning";
import { WatersResponse } from "../../config/endpoints/waters";
import useSessionContext from "../../contexts/Session/useSessionContext";
import { showToast } from "../../contexts/Toast";
import useOrganizationSender from "../../hooks/api/organizations/UseOrganizationSender/UseOrganizationSender";
import useOrganizationSettings from "../../hooks/api/organizations/UseOrganizationSettings/UseOrganizationSettings";
import useWatersUploader from "../../hooks/api/UseWatersUploader/UseWatersUploader";
import useAppInsightsContext from "../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import { EventName } from "../../utils/AppInsights";
import { parseCsvForError } from "../../utils/FileUtils";
import { getClientHeader } from "../../utils/SessionStorageTools";
import { FileType } from "../../utils/TemporarySettingsAPITypes";
import Spinner from "../Spinner";

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
    const appInsights = useAppInsightsContext();
    const { data: organization } = useOrganizationSettings();
    const { data: senderDetail } = useOrganizationSender();
    const { activeMembership, rsConsole } = useSessionContext();
    const fileInputRef = useRef<FileInputRef>(null);
    const { format } = selectedSchemaOption;
    const accept = selectedSchemaOption ? `.${format.toLowerCase()}` : BASE_ACCEPT_VALUE;

    const { mutateAsync: sendFile, isPending: isUploading } = useWatersUploader();

    async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
        // TODO: consolidate with upcoming FileUtils generic function
        if (!event?.target?.files?.length) {
            onFileSubmitError();
            return;
        }
        const selectedFile = event.target.files.item(0)!;

        const selectedFileContent = await selectedFile.text();

        if (selectedFile.type === "csv" || selectedFile.type === "text/csv") {
            const localCsvError = parseCsvForError(selectedFile.name, selectedFileContent);
            if (localCsvError) {
                showToast(localCsvError, "error");
                return;
            }
        }

        onFileChange(selectedFile, selectedFileContent);
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (fileContent.length === 0 || file?.name == null) {
            showToast("No file contents to validate", "error");
            return;
        }

        let eventData;
        try {
            const response = await sendFile({
                contentType,
                fileContent,
                fileName: file.name,
                client: getClientHeader(selectedSchemaOption.value, activeMembership, senderDetail ?? undefined),
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

            showToast("File validation error. Please try again.", "error");

            onFileSubmitError();
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

    if (senderDetail == null) rsConsole.error(new Error("Failed to fetch sender detail"));

    return (
        <div>
            <FileHandlerPiiWarning />

            {(() => {
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
                                        Checking your file for any errors that will prevent your data from being
                                        reported successfully...
                                    </p>
                                </div>
                            </div>
                        </div>
                    );
                }

                const prompt = UPLOAD_PROMPT_DESCRIPTIONS[format];

                return (
                    <Form name="fileValidation" onSubmit={(ev) => void handleSubmit(ev)} className="rs-full-width-form">
                        <FormGroup className="margin-top-0">
                            <Label className="font-sans-xs" id="upload-csv-input-label" htmlFor="upload-csv-input">
                                <span className="display-block">{prompt.title}</span>
                                <span className="display-block text-base">{prompt.subtitle}</span>
                            </Label>
                            <FileInput
                                id="upload-csv-input"
                                name="upload-csv-input"
                                aria-describedby="upload-csv-input-label"
                                data-testid="upload-csv-input"
                                onChange={(ev) => void handleFileChange(ev)}
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
                            <Button disabled={!isValid} className="usa-button" type="submit">
                                Submit
                            </Button>
                        </div>
                    </Form>
                );
            })()}
        </div>
    );
}
