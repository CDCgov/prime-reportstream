import {
    Button,
    ButtonGroup,
    Modal,
    ModalFooter,
    ModalHeading,
    ModalRef,
    ModalToggleButton,
} from "@trussworks/react-uswds";
import classNames from "classnames";
import {
    ComponentProps,
    forwardRef,
    Ref,
    useImperativeHandle,
    useRef,
} from "react";

interface ModalConfirmDialogProps extends ComponentProps<typeof Modal> {
    actionButton: JSX.Element;
    heading: JSX.Element;
    onCancel?: () => void;
}

/**
 * Modal proxy that applies explicit layout and style.
 */
export const ModalConfirmDialog = forwardRef(
    (
        {
            id,
            actionButton,
            heading,
            children,
            onCancel,
            ...props
        }: ModalConfirmDialogProps,
        ref: Ref<ModalRef | null>,
    ) => {
        const modalRef = useRef<ModalRef | null>(null);
        useImperativeHandle(ref, () => modalRef.current, [modalRef]);
        return (
            <Modal
                ref={modalRef}
                id={id}
                data-testid={id}
                aria-labelledby={`${id}-heading`}
                aria-describedby={`${id}-description`}
                {...props}
            >
                <ModalHeading
                    id={`${id}-heading`}
                    data-testid={`${id}-heading`}
                >
                    {heading}
                </ModalHeading>
                <div className="usa-prose">{children}</div>
                <ModalFooter>
                    <ButtonGroup>
                        <ModalToggleButton
                            modalRef={modalRef}
                            closer
                            type="button"
                            outline={true}
                            className="padding-105 text-center"
                            onClick={() => onCancel?.()}
                        >
                            Cancel
                        </ModalToggleButton>
                        {actionButton}
                    </ButtonGroup>
                </ModalFooter>
            </Modal>
        );
    },
);

/**
 * Button proxy to apply explicit styling.
 */
export function ModalConfirmButton({
    className,
    ...props
}: ComponentProps<typeof Button>) {
    const classname = classNames(className, "padding-105 text-center");
    return <Button {...props} className={classname} />;
}
