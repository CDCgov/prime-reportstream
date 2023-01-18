import { IconHelp, Tooltip } from "@trussworks/react-uswds";

import {
    SampleFilterObject,
    SampleKeysObj,
    SampleTimingObj,
    SampleTranslationObj,
    SampleTransportObject,
} from "../../utils/TemporarySettingsAPITypes";

interface ObjectTooltipProps {
    obj:
        | SampleTimingObj
        | SampleKeysObj
        | SampleTranslationObj
        | SampleFilterObject
        | SampleTransportObject;
}

interface EnumTooltipProps {
    vals: Array<string>;
}

const copyToClipboard = (text: string) =>
    navigator.clipboard.writeText(text).then(() => console.log("Copied"));

const ObjectTooltip = ({ obj }: ObjectTooltipProps) => {
    return (
        <Tooltip
            className="fixed-tooltip"
            position="right"
            label={`${obj.stringify()}\n\n\n${obj.description()}`}
            onClick={() => copyToClipboard(obj.stringify())}
        >
            <IconHelp />
        </Tooltip>
    );
};

const EnumTooltip = ({ vals }: EnumTooltipProps) => {
    const formattedVals = `${vals.map((val) => `\t${val}\n`)}`;
    const label = `Available values:\n${formattedVals}`;
    const clipboard = `${vals.map((val) => {
        return ` ${val}`; // added whitespace
    })}`;
    return (
        <Tooltip
            position="bottom"
            label={label}
            onClick={() => copyToClipboard(clipboard)}
        >
            <IconHelp />
        </Tooltip>
    );
};

export { ObjectTooltip, EnumTooltip };
