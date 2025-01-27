import { Button, Icon } from "@trussworks/react-uswds";
import { type PropsWithChildren, useCallback } from "react";
import openAsBlob from "../../../utils/OpenAsBlob/OpenAsBlob";

export interface TestMessageLabelProps extends PropsWithChildren {
    data: string;
}

const TestMessageLabel = ({ data, children }: TestMessageLabelProps) => {
    const handleClick = useCallback(() => openAsBlob(data), [data]);
    return (
        <>
            {children}{" "}
            <Button type="button" unstyled onClick={handleClick}>
                View message
                <Icon.Visibility className="text-tbottom margin-left-05" aria-label="View message" />
            </Button>
        </>
    );
};

export default TestMessageLabel;
