import React from "react";
import { Button } from "@trussworks/react-uswds";

type FileHandlerSubmitButtonProps = {
    submitted: boolean;
    disabled: boolean;
    reset: () => void;
    resetText: string;
    submitText: string;
};

export const FileHandlerSubmitButton = ({
    submitted,
    disabled,
    reset,
    resetText,
    submitText,
}: FileHandlerSubmitButtonProps) => {
    if (submitted) {
        return (
            <Button type="button" onClick={reset}>
                {resetText}
            </Button>
        );
    }
    return (
        <Button disabled={disabled} type={"submit"}>
            {submitText}
        </Button>
    );
};
