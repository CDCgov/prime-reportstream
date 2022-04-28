import { IconHelp, Tooltip } from "@trussworks/react-uswds";

import {
    SampleFilterObject,
    SampleKeysObj,
    SampleTimingObj,
    SampleTranslationObj,
} from "../../utils/TemporarySettingsAPITypes";

interface ObjectTooltipProps {
    obj:
        | SampleTimingObj
        | SampleKeysObj
        | SampleTranslationObj
        | SampleFilterObject;
}

interface EnumTooltipProps {
    vals: Array<string>;
}

const ObjectTooltip = ({ obj }: ObjectTooltipProps) => {
    return (
        <Tooltip
            className="fixed-tooltip"
            position="right"
            label={`${obj.stringify()}\n\n\n${obj.description()}`}
        >
            <IconHelp />
        </Tooltip>
    );
};

const EnumTooltip = ({ vals }: EnumTooltipProps) => {
    const formattedVals = `${vals.map((val) => `\t${val}\n`)}`;
    const label = `Available values:\n${formattedVals}`;
    return (
        <Tooltip className="fixed-tooltip" position="right" label={label}>
            <IconHelp />
        </Tooltip>
    );
};

export { ObjectTooltip, EnumTooltip };
