import React from "react";
import { Button } from "@trussworks/react-uswds";

type FileHandlerSubmitButtonProps = {
    isSubmitting: boolean;
    submitted: boolean;
    disabled: boolean;
    reset: () => void;
};

export const FileHandlerSubmitButton = ({
    isSubmitting,
    submitted,
    disabled,
    reset,
}: FileHandlerSubmitButtonProps) => {
    if (isSubmitting) {
        return null;
    }
    if (submitted) {
        return (
            <Button type="button" onClick={reset}>
                Validate another file
            </Button>
        );
    }
    return (
        <Button type="submit" disabled={disabled}>
            Validate
        </Button>
    );
};
