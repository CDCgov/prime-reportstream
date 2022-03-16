import React, { FC } from "react";

import { CheckFeatureFlag } from "../../pages/misc/FeatureFlags";
import { EditableCompare } from "../EditableCompare";

export const DiffEditorComponent: FC<{
    originalCode: string;
    modifiedCode: string;
}> = ({ modifiedCode, originalCode }) => {
    const showNewDiffEditor = CheckFeatureFlag("showNewDiffEditor");
    if (showNewDiffEditor) {
        return (
            <EditableCompare original={originalCode} modified={modifiedCode} />
        );
    } else {
        return (
            <div>
                The diff editor is currently under maintenance. Please click
                Save below to save your changes
            </div>
        );
    }
};
