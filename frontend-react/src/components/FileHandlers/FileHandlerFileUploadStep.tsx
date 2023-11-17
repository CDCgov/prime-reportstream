import React, { useRef } from "react";
import {
    Button,
    Form,
    FormGroup,
    Label,
    FileInput,
    FileInputRef,
} from "@trussworks/react-uswds";

import { RSSender } from "../../config/endpoints/settings";
import Spinner from "../Spinner";
import { FileType } from "../../utils/TemporarySettingsAPITypes";
import { MembershipSettings } from "../../utils/OrganizationUtils";
import { FileHandlerStepProps } from "../../pages/file-handler/FileHandler";

import FileHandlerPiiWarning from "./FileHandlerPiiWarning";

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

export interface FileHandlerFileUploadStepProps
    extends Pick<FileHandlerStepProps, "isValid" | "selectedSchemaOption"> {
    onFileChange: ((e: React.ChangeEvent<HTMLInputElement>) => void) &
        React.ChangeEventHandler<HTMLInputElement>;
    onBack: React.MouseEventHandler<HTMLButtonElement>;
    isSubmitting?: boolean;
    onSubmit: ((event: React.FormEvent<HTMLFormElement>) => void) &
        React.FormEventHandler<HTMLFormElement>;
}

const BASE_ACCEPT_VALUE = [".csv", ".hl7"].join(",");

export default function FileHandlerFileUploadStep({
    isValid,
    onFileChange,
    onBack,
    selectedSchemaOption,
    isSubmitting,
    onSubmit,
}: FileHandlerFileUploadStepProps) {
    const fileInputRef = useRef<FileInputRef>(null);
    const { format } = selectedSchemaOption;
    const accept = selectedSchemaOption
        ? `.${format.toLowerCase()}`
        : BASE_ACCEPT_VALUE;

    return (
        <div>
            <FileHandlerPiiWarning />

            {(() => {
                if (isSubmitting) {
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
                        onSubmit={onSubmit}
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
                                onChange={onFileChange}
                                required
                                ref={fileInputRef}
                                accept={accept}
                                crossOrigin={undefined}
                            />
                        </FormGroup>

                        <div className="margin-top-05 margin-bottom-0 display-flex">
                            <Button
                                className="usa-button usa-button--outline"
                                type={"button"}
                                onClick={onBack}
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
