import {
    Button,
    ButtonGroup,
    Modal,
    ModalFooter,
    ModalHeading,
    ModalRef,
} from "@trussworks/react-uswds";
import { useRef } from "react";
import classnames from "classnames";

import { OverallStatus } from "../../config/endpoints/waters";
import { ErrorType } from "../../hooks/UseFileHandler";

import {
    FileQualityFilterDisplay,
    RequestedChangesDisplay,
    RequestLevel,
} from "./FileHandlerMessaging";
import { FileHandlerStepProps } from "./FileHandler";

const SERVER_ERROR_MESSAGING = {
    heading: OverallStatus.ERROR,
    message: "There was a server error. Your file has not been accepted.",
};

const ERROR_MESSAGING_MAP = {
    server: SERVER_ERROR_MESSAGING,
    file: {
        heading: "File did not pass validation",
        message: "Resubmit with the required edits.",
    },
};

export interface FileHandlerErrorsWarningsStepProps
    extends FileHandlerStepProps {
    onTestAnotherFileClick: () => void;
}

export default function FileHandlerErrorsWarningsStep({
    destinations,
    errorType,
    errors,
    file,
    onNextStepClick,
    onTestAnotherFileClick,
    reportItems,
    selectedSchemaOption,
    warnings,
}: FileHandlerErrorsWarningsStepProps) {
    // default to FILE messaging here, partly to simplify typecheck
    const errorMessaging = ERROR_MESSAGING_MAP[errorType || ErrorType.FILE];

    // Array containing only qualityFilterMessages that have filteredReportItems.
    const qualityFilterMessages = reportItems?.filter(
        (d) => d.filteredReportItems.length > 0,
    );

    const hasQualityFilterMessages =
        destinations.length > 0 &&
        qualityFilterMessages &&
        qualityFilterMessages.length > 0;

    const modalRef = useRef<ModalRef>(null);
    return (
        <div className="file-handler-table">
            {hasQualityFilterMessages ? (
                <FileQualityFilterDisplay
                    destinations={qualityFilterMessages}
                    heading="Jurisdictional errors found"
                    message="Your file does not meet the data model for the following jurisdiction(s). Resolve the errors below to ensure those jurisdictions can receive your data."
                />
            ) : (
                <>
                    {errors.length > 0 && (
                        <RequestedChangesDisplay
                            title={RequestLevel.ERROR}
                            data={errors}
                            message={errorMessaging.message}
                            heading={errorMessaging.heading}
                            schemaColumnHeader={selectedSchemaOption.format}
                            file={file}
                        />
                    )}
                    {warnings.length > 0 && (
                        <RequestedChangesDisplay
                            title={RequestLevel.WARNING}
                            data={warnings}
                            message="To avoid problems when sending files later, we strongly recommend fixing these issues now."
                            heading="Recommended edits found"
                            schemaColumnHeader={selectedSchemaOption.format}
                            file={file}
                        />
                    )}
                </>
            )}

            <div className="display-flex margin-bottom-2">
                <Button
                    className={classnames("usa-button", {
                        "usa-button--outline": !errors.length,
                    })}
                    type="button"
                    onClick={onTestAnotherFileClick}
                >
                    Test another file
                </Button>
                {!errors.length && (
                    <Button
                        className="usa-button"
                        type="button"
                        onClick={() => modalRef?.current?.toggleModal()}
                    >
                        Continue without changes
                    </Button>
                )}
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
                            className="usa-button usa-button--outline"
                            type={"button"}
                            onClick={() => modalRef?.current?.toggleModal()}
                        >
                            Go back and save
                        </Button>
                        <Button
                            className="usa-button"
                            type={"button"}
                            onClick={onNextStepClick}
                        >
                            Continue
                        </Button>
                    </ButtonGroup>
                </ModalFooter>
            </Modal>
        </div>
    );
}
