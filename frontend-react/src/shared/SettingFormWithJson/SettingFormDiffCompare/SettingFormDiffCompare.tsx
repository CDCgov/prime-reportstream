import { checkJsonHybrid } from "../../../hooks/UseObjectJsonHybridEdit/UseObjectJsonHybridEdit";
import { DiffCompare } from "../../DiffCompare/DiffCompare";

export function SettingFormDiffCompare({ initial, current, jsonKeys }: any) {
    const finalCurrent = checkJsonHybrid(current, jsonKeys);
    return <DiffCompare a={initial} b={finalCurrent} />;
}
