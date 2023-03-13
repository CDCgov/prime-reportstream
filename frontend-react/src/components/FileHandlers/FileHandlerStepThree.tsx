import {
    Button,
    ButtonGroup,
    Modal,
    ModalFooter,
    ModalHeading,
    ModalRef,
    ModalToggleButton,
} from "@trussworks/react-uswds";
import { useRef } from "react";
import { ResponseError } from "../../config/endpoints/waters";
import { Destination } from "../../resources/ActionDetailsResource";
import { SchemaOption } from "../../senders/hooks/UseSenderSchemaOptions";
import { ModalConfirmSaveButton } from "../Admin/CompareJsonModal";

import {
    FileQualityFilterDisplay,
    FileSuccessDisplay,
    RequestedChangesDisplay,
    RequestLevel,
} from "./FileHandlerMessaging";

interface FileHandlerStepThreeProps {
    destinations?: string;
    errorMessaging: { message: string; heading: string };
    errors: ResponseError[];
    hasQualityFilterMessages: boolean | undefined;
    isFileSuccess: string | boolean;
    qualityFilterMessages: Destination[] | undefined;
    reportId?: string;
    successDescription: string;
    successTimestamp?: string;
    warnings: ResponseError[];
    selectedSchemaOption: SchemaOption | null;
    handlePrevFileHandlerStep: () => void;
    handleNextFileHandlerStep: () => void;
}

export const FileHandlerStepThree = ({
    destinations,
    errorMessaging,
    errors,
    hasQualityFilterMessages,
    isFileSuccess,
    qualityFilterMessages,
    reportId,
    successDescription,
    successTimestamp,
    warnings,
    selectedSchemaOption,
    handlePrevFileHandlerStep,
    handleNextFileHandlerStep,
}: FileHandlerStepThreeProps) => {
    const modalRef = useRef<ModalRef>(null);
    return (
        <div className="file-handler-table">
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
            {errors.length > 0 && (
                <RequestedChangesDisplay
                    title={RequestLevel.ERROR}
                    data={errors}
                    message={errorMessaging.message}
                    heading={errorMessaging.heading}
                    schemaColumnHeader={selectedSchemaOption.format}
                />
            )}
            {warnings.length > 0 && (
                <RequestedChangesDisplay
                    title={RequestLevel.WARNING}
                    data={warnings}
                    message="To avoid problems when sending files later, we strongly recommend fixing these issues now."
                    heading="Recommended edits found"
                    schemaColumnHeader={selectedSchemaOption.format}
                />
            )}

            {hasQualityFilterMessages && (
                <FileQualityFilterDisplay
                    destinations={qualityFilterMessages}
                    heading=""
                    message={`The file does not meet the jurisdiction's schema. Please resolve the errors below.`}
                />
            )}
            <div className="grid-col display-flex">
                <Button
                    className="usa-button flex-align-self-start height-5 margin-top-4 usa-button--outline"
                    type={"button"}
                    onClick={handlePrevFileHandlerStep}
                >
                    Test another file
                </Button>
                <Button
                    disabled={errors.length > 0}
                    className="usa-button flex-align-self-start height-5 margin-top-4"
                    type={"button"}
                    onClick={() => modalRef?.current?.toggleModal()}
                >
                    Continue without changes
                </Button>
            </div>
            <Modal id="file-validator-modal" ref={modalRef}>
                <ModalHeading>
                    Have you exported your recommended edits?
                </ModalHeading>
                <div className="usa-prose">
                    <p>
                        Before continuing to validation, we suggest saving your
                        recommended edits so our team can better assist you with
                        your files.
                    </p>
                </div>
                <ModalFooter>
                    <ButtonGroup>
                        <Button
                            className="usa-button flex-align-self-start height-5 margin-top-4 usa-button--outline"
                            type={"button"}
                            onClick={() => modalRef?.current?.toggleModal()}
                        >
                            Go back and save
                        </Button>
                        <Button
                            className="usa-button flex-align-self-start height-5 margin-top-4"
                            type={"button"}
                            onClick={handleNextFileHandlerStep}
                        >
                            Continue
                        </Button>
                    </ButtonGroup>
                </ModalFooter>
            </Modal>
        </div>
    );
};
