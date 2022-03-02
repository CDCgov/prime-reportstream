import {
    Button,
    ButtonGroup,
    Modal,
    ModalFooter,
    ModalHeading,
    ModalRef,
    ModalToggleButton,
} from "@trussworks/react-uswds";
import React, { RefObject } from "react";
import { ButtonProps } from "@trussworks/react-uswds/lib/components/Button/Button";

import { DiffEditorComponent } from "./DiffEditorComponent";

interface ModalConfirmButtonProps {
    uniquid: string;
    handleClose: () => void;
}

export const ModalConfirmSaveButton = ({
    handleClose,
    uniquid,
    ...buttonProps
}: ModalConfirmButtonProps &
    Omit<ButtonProps, "type"> &
    JSX.IntrinsicElements["button"]): React.ReactElement => {
    return (
        <Button
            {...buttonProps}
            aria-label="Confirm saving this item"
            onClick={handleClose}
            data-close-modal
            key={`${uniquid}-confirm-button`}
            data-uniqueid={uniquid}
            type="button"
        >
            {buttonProps.children}
        </Button>
    );
};

ModalConfirmSaveButton.displayName = "ModalConfirmSaveButton";

interface CompareSettingsModalProps {
    uniquid: string;
    onConfirm: () => void;
    modalRef: RefObject<ModalRef>;
    oldjson: string;
    newjson: string;
    handleEditorDidMount: (editor: null) => void;
}

export const ConfirmSaveSettingModal: React.FC<CompareSettingsModalProps> = ({
    uniquid,
    onConfirm,
    modalRef,
    oldjson,
    newjson,
    handleEditorDidMount,
}) => {
    const scopedConfirm = () => {
        modalRef?.current?.toggleModal(undefined, false);
        onConfirm();
    };

    return (
        <>
            <Modal
                ref={modalRef}
                id={uniquid}
                aria-labelledby={`${uniquid}-heading`}
                aria-describedby={`${uniquid}-description`}
            >
                <ModalHeading id={`${uniquid}-heading`}>
                    Compare your changes with previous version
                </ModalHeading>
                <div className="usa-prose">
                    <p id={`${uniquid}-description`}>
                        You are about to change this setting: {uniquid}
                    </p>
                    <DiffEditorComponent
                        originalCode={oldjson}
                        modifiedCode={newjson}
                        language={"JSON"}
                        mounter={handleEditorDidMount}
                    />
                </div>
                <ModalFooter>
                    <ButtonGroup>
                        <ModalConfirmSaveButton
                            uniquid={uniquid}
                            handleClose={scopedConfirm}
                        >
                            Save
                        </ModalConfirmSaveButton>
                        <ModalToggleButton
                            modalRef={modalRef}
                            closer
                            unstyled
                            className="padding-105 text-center"
                        >
                            Go back
                        </ModalToggleButton>
                    </ButtonGroup>
                </ModalFooter>
            </Modal>
        </>
    );
};
