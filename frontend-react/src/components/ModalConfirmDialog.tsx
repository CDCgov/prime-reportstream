import {
    Button,
    ButtonGroup,
    Modal,
    ModalFooter,
    ModalHeading,
    ModalRef,
    ModalToggleButton,
} from "@trussworks/react-uswds";
import React, {
    forwardRef,
    Ref,
    useImperativeHandle,
    useRef,
    useState,
} from "react";

/**
 * Wrapper on top of ModalRef to do simple confirmation modal.
 * Step 1 - Add component to end of page:
 *             <ModalConfirmDialog
 *                 id={"deleteConfirm"}
 *                 onConfirm={confirmDelete}
 *                 ref={modalRef}
 *             ></ModalConfirmDialog>
 *
 * Step 2 - Set up handler to display the dialog.
 *          Note the messages are supplied here instead of step 1 for flexibility.
 *    const modalRef = useRef<ModalConfirmRef>(null);
 *    const ShowDeleteConfirm = (itemId: string) => {
 *         modalRef?.current?.showModal({
 *             title: "Confirm Delete",
 *             message:
 *                 "Deleting a setting will only mark it deleted. It can be accessed via the revision history",
 *             okButtonText: "Delete",
 *             itemId: itemId,
 *         });
 *    const confirmDelete = async (deleteItemId: string) => {
 *    // handle Confirm Button click action here
 *    };
 */

interface ModalConfirmProps {
    id: string;
    /** called back with the itemId passed to it*/
    onConfirm: (itemId: string) => void;
    isLarge?: boolean;
}

type ModalConfirmSettings = {
    title: string;
    message: string;
    okButtonText: string;
    itemId: string;
};

export interface ModalConfirmRef extends ModalRef {
    showModal: (props: ModalConfirmSettings) => void;
    hideModal: () => void;
}

// used for initial state or when modal is closed.
const blankSettings = {
    title: "",
    message: "",
    okButtonText: "ok",
    itemId: "",
};

export const ModalConfirmDialog = forwardRef(
    (
        { id, onConfirm, isLarge }: ModalConfirmProps,
        ref: Ref<ModalConfirmRef>,
    ) => {
        const modalRef = useRef<ModalRef>(null);
        const [modalState, setModalState] =
            useState<ModalConfirmSettings>(blankSettings);

        // Functions that can be called on the instance
        useImperativeHandle(
            ref,
            () => ({
                showModal: (modalSettings: ModalConfirmSettings) => {
                    setModalState(modalSettings);
                    modalRef?.current?.toggleModal(undefined, true);
                },
                hideModal: () => {
                    setModalState(blankSettings);
                    modalRef?.current?.toggleModal(undefined, false);
                },
                // route these down to modal ref
                modalId: modalRef?.current?.modalId || "",
                modalIsOpen: modalRef?.current?.modalIsOpen || false,
                toggleModal: modalRef?.current?.toggleModal || (() => false),
            }),
            [modalRef],
        );

        const onClickHandler = () => {
            const itemId = modalState.itemId; // about to clear it so save
            setModalState(blankSettings);
            modalRef?.current?.toggleModal(undefined, false);
            onConfirm(itemId);
        };

        return (
            <>
                <Modal
                    ref={modalRef}
                    id={id}
                    data-testid={id}
                    aria-labelledby={`${id}-heading`}
                    aria-describedby={`${id}-description`}
                    isLarge={isLarge === true}
                >
                    <ModalHeading
                        id={`${id}-heading`}
                        data-testid={`${id}-heading`}
                    >
                        {modalState.title}
                    </ModalHeading>
                    <div className="usa-prose">
                        <p id={`${id}-description`}>{modalState.message}</p>
                    </div>
                    <ModalFooter>
                        <ButtonGroup>
                            <ModalToggleButton
                                modalRef={modalRef}
                                closer
                                type="button"
                                outline={true}
                                className="padding-105 text-center"
                            >
                                Cancel
                            </ModalToggleButton>
                            <Button
                                data-testid={`${id}-closebtn`}
                                type="button"
                                className="padding-105 text-center"
                                onClick={() => onClickHandler()}
                            >
                                {modalState.okButtonText}
                            </Button>
                        </ButtonGroup>
                    </ModalFooter>
                </Modal>
            </>
        );
    },
);
