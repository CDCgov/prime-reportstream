import React from "react";
import { Button } from "@trussworks/react-uswds";

type FileHandlerSubmitButtonProps = {
    isSubmitting: boolean;
    submitted: boolean;
    disabled: boolean;
    reset: () => void;
    resetText: string;
    submitText: string;
};

export const FileHandlerSubmitButton = ({
    isSubmitting,
    submitted,
    disabled,
    reset,
    resetText,
    submitText,
}: FileHandlerSubmitButtonProps) => {
    if (isSubmitting) {
        return null;
    }
    if (submitted) {
        return (
            <Button type="button" onClick={reset}>
                {resetText}
            </Button>
        );
    }
    return (
        <Button type="submit" disabled={disabled}>
            {submitText}
        </Button>
    );
};
