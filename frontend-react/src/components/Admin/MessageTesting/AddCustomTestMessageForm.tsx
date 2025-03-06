import { Button, Textarea } from "@trussworks/react-uswds";
import { type ComponentProps, type FormEventHandler, useCallback, useState } from "react";

export interface AddCustomMessageFormProps extends ComponentProps<"form"> {
    onCancel: () => void;
}

export interface AddCustomMessageFormValues {
    customMessageTestBody: string;
}

const AddCustomTestMessageFormProps = ({ onCancel, onChange, ...props }: AddCustomMessageFormProps) => {
    const [isSubmitEnabled, setIsSubmitEnabled] = useState(false);

    const handleFormChange = useCallback<FormEventHandler<HTMLFormElement>>(
        (e) => {
            setIsSubmitEnabled(e.currentTarget.checkValidity());
            onChange?.(e);
        },
        [onChange],
    );

    return (
        <form className="width-full" onChange={handleFormChange} {...props}>
            <p className="text-bold">Enter custom message</p>
            <p>Custom messages do not save to the bank after you log out.</p>
            <Textarea
                id="custom-message-test-body"
                name="customMessageTestBody"
                className="width-full maxw-full margin-bottom-205"
                required={true}
            />
            <div className="width-full text-right">
                <Button
                    type="button"
                    outline
                    onClick={() => {
                        onCancel();
                    }}
                >
                    Cancel
                </Button>
                <Button type="submit" disabled={!isSubmitEnabled}>
                    Add
                </Button>
            </div>
        </form>
    );
};

export default AddCustomTestMessageFormProps;
