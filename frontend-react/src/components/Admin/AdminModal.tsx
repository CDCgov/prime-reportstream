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

interface ModalConfirmButtonProps {
    uniquid: string;
    handleClose: () => void;
}

export const ModalConfirmDeleteButton = ({
    handleClose,
    uniquid,
    ...buttonProps
}: ModalConfirmButtonProps &
    Omit<ButtonProps, "type"> &
    JSX.IntrinsicElements["button"]): React.ReactElement => {
    return (
        <Button
            {...buttonProps}
            aria-label="Confirm deleting this item"
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

ModalConfirmDeleteButton.displayName = "ModalConfirmDeleteButton";

interface DeleteSettingsModalProps {
    uniquid: string;
    onConfirm: () => void;
    modalRef: RefObject<ModalRef>;
}

export const ConfirmDeleteSettingModal: React.FC<DeleteSettingsModalProps> = ({
    uniquid,
    onConfirm,
    modalRef,
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
                    Are you sure you want to continue?
                </ModalHeading>
                <div className="usa-prose">
                    <p id={`${uniquid}-description`}>
                        You are about to delete this setting: {uniquid}
                    </p>
                </div>
                <ModalFooter>
                    <ButtonGroup>
                        <ModalConfirmDeleteButton
                            uniquid={uniquid}
                            handleClose={scopedConfirm}
                        >
                            Delete
                        </ModalConfirmDeleteButton>
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
