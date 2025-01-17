import { Button, Icon, Radio } from "@trussworks/react-uswds";
import { ChangeEvent } from "react";
import openAsBlob from "../../../utils/OpenAsBlob/OpenAsBlob";

export const MessageTestingRadioField = ({
    title,
    body,
    index,
    handleSelect,
    selectedOption,
}: {
    title: string;
    body: string;
    index: number;
    handleSelect: (event: ChangeEvent<HTMLInputElement>) => void;
    selectedOption: string | null;
}) => {
    return (
        <Radio
            id={`message-${index}`}
            name="message-test-form"
            value={body}
            onChange={handleSelect}
            checked={selectedOption === body}
            className="usa-radio bg-base-lightest padding-2 border-bottom-1px border-gray-30"
            label={
                <>
                    {" "}
                    {title}{" "}
                    <Button type="button" unstyled onClick={() => openAsBlob(body)}>
                        View message
                        <Icon.Visibility className="text-tbottom margin-left-05" aria-label="View message" />
                    </Button>
                </>
            }
        />
    );
};
