import { Button, Icon, Radio } from "@trussworks/react-uswds";
import { ChangeEvent } from "react";

export const RadioField = ({
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
    const openTextInNewTab = () => {
        let formattedContent = body;

        // Check if the content is JSON and format it
        try {
            formattedContent = JSON.stringify(JSON.parse(body), null, 2);
        } catch {
            formattedContent = body;
        }

        const blob = new Blob([formattedContent], { type: "text/plain" });

        const url = URL.createObjectURL(blob);

        window.open(url, "_blank");

        // Revoke the URL to free up memory
        URL.revokeObjectURL(url);
    };

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
                    <Button type="button" unstyled onClick={openTextInNewTab}>
                        View message
                        <Icon.Visibility className="text-tbottom margin-left-05" aria-label="View message" />
                    </Button>
                </>
            }
        />
    );
};
