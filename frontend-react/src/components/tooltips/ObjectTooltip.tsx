import { Icon, Tooltip } from "@trussworks/react-uswds";

import {
    SampleFilterObject,
    SampleKeysObj,
    SampleTimingObj,
    SampleTranslationObj,
    SampleTransportObject,
} from "../../utils/TemporarySettingsAPITypes";

interface ObjectTooltipProps {
    obj: SampleTimingObj | SampleKeysObj | SampleTranslationObj | SampleFilterObject | SampleTransportObject;
}

interface EnumTooltipProps {
    vals: string[];
}

const copyToClipboard = (text: string) => navigator.clipboard.writeText(text);

const ObjectTooltip = ({ obj }: ObjectTooltipProps) => {
    return (
        <Tooltip
            className="fixed-tooltip"
            position="right"
            label={`${obj.stringify()}\n\n\n${obj.description()}`}
            onClick={() => void copyToClipboard(obj.stringify())}
        >
            <Icon.Help />
        </Tooltip>
    );
};

const EnumTooltip = ({ vals }: EnumTooltipProps) => {
    const formattedVals = `${vals.map((val) => `\t${val}`).join("\n")}`;
    const label = `Available values:\n${formattedVals}`;
    const clipboard = `${vals.join(" ")}`;
    return (
        <Tooltip position="bottom" label={label} onClick={() => void copyToClipboard(clipboard)}>
            <Icon.Help />
        </Tooltip>
    );
};

export { ObjectTooltip, EnumTooltip };
