import {
    Button,
    ButtonGroup,
    Modal,
    ModalFooter,
    ModalHeading,
    ModalRef,
    ModalToggleButton,
} from "@trussworks/react-uswds";
import React, { forwardRef, Ref, useImperativeHandle, useRef } from "react";
import { ButtonProps } from "@trussworks/react-uswds/lib/components/Button/Button";

import { EditableCompare, EditableCompareRef } from "../EditableCompare";

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
            key={`${uniquid}-confirm-button`}
            data-uniqueid={uniquid}
            type="button"
        >
            {buttonProps.children}
        </Button>
    );
};

ModalConfirmSaveButton.displayName = "ModalConfirmSaveButton";

// interface on Component that is callable
export interface ConfirmSaveSettingModalRef extends ModalRef {
    getEditedText: () => string;
    showModal: () => void;
    hideModal: () => void;
}

interface CompareSettingsModalProps {
    uniquid: string;
    onConfirm: () => void;
    oldjson: string;
    newjson: string;
}

export const ConfirmSaveSettingModal = forwardRef(
    (
        { uniquid, onConfirm, oldjson, newjson }: CompareSettingsModalProps,
        ref: Ref<ConfirmSaveSettingModalRef>
    ) => {
        const modalRef = useRef<ModalRef>(null);
        const diffEditorRef = useRef<EditableCompareRef>(null);

        const scopedConfirm = () => {
            onConfirm();
        };

        useImperativeHandle(
            ref,
            () => ({
                // route this down the diffEditor
                getEditedText: (): string => {
                    return diffEditorRef?.current?.getEditedText() || newjson;
                },
                showModal: () => {
                    // need to refresh data passed into object
                    debugger;
                    diffEditorRef?.current?.refeshEditedText(newjson);
                    modalRef?.current?.toggleModal(undefined, true);
                },
                hideModal: () => {
                    modalRef?.current?.toggleModal(undefined, false);
                },
                // route these down to modal ref
                modalId: modalRef?.current?.modalId || "",
                modalIsOpen: modalRef?.current?.modalIsOpen || false,
                toggleModal:
                    modalRef?.current?.toggleModal || ((e, o) => false),
            }),
            [diffEditorRef, newjson, modalRef]
        );

        return (
            <>
                <Modal
                    ref={modalRef}
                    id={uniquid}
                    aria-labelledby={`${uniquid}-heading`}
                    aria-describedby={`${uniquid}-description`}
                    isLarge={true}
                    className="rs-compare-modal"
                >
                    <ModalHeading id={`${uniquid}-heading`}>
                        Compare your changes with previous version
                    </ModalHeading>
                    <div className="usa-prose">
                        <p id={`${uniquid}-description`}>
                            You are about to change this setting: {uniquid}
                        </p>
                        <EditableCompare
                            ref={diffEditorRef}
                            original={oldjson}
                            modified={newjson}
                        />
                    </div>
                    <ModalFooter>
                        <ButtonGroup>
                            <ModalToggleButton
                                modalRef={modalRef}
                                closer
                                unstyled
                                className="padding-105 text-center"
                            >
                                Go back
                            </ModalToggleButton>
                            <ModalConfirmSaveButton
                                uniquid={uniquid}
                                handleClose={scopedConfirm}
                            >
                                Save
                            </ModalConfirmSaveButton>
                        </ButtonGroup>
                    </ModalFooter>
                </Modal>
            </>
        );
    }
);
