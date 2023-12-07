import { Icon, Tooltip } from "@trussworks/react-uswds";
import { useMemo } from "react";

const copyToClipboard = (text: string) => navigator.clipboard.writeText(text);

export interface ObjectExampleTooltipProps {
    obj: object;
    description?: string;
}

export default function ObjectExampleTooltip({
    obj,
    description,
}: ObjectExampleTooltipProps) {
    const str = useMemo(() => JSON.stringify(obj, undefined, 2), [obj]);
    const labelStart = description ? `${description}\n\n\n` : "";
    return (
        <Tooltip
            className="fixed-tooltip"
            position="right"
            label={`${labelStart}Click to copy a sample to your clipboard.`}
        >
            <Icon.Help onClick={() => copyToClipboard(str)} />
        </Tooltip>
    );
}
