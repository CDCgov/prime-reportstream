import React, { FC, useState } from "react";
import { DiffEditor } from "@monaco-editor/react";

import { CheckFeatureFlag } from "../../pages/misc/FeatureFlags";
import { EditableCompare } from "../EditableCompare";

export const DiffEditorComponent: FC<{
    originalCode: string;
    modifiedCode: string;
    language: string;
    mounter: (editor: null, monaco: any) => void;
}> = ({ modifiedCode, originalCode, language, mounter }) => {
    const showDiffEditor = CheckFeatureFlag("showDiffEditor");
    const showNewDiffEditor = CheckFeatureFlag("showNewDiffEditor");
    const [modifiedCodeStr, setModifiedCodeStr] = useState(modifiedCode);
    if (showDiffEditor) {
        return (
            <DiffEditor
                height="50vh"
                className="w-full h-full"
                theme="vs-dark"
                language={language}
                options={{ minimap: { enabled: false } }}
                original={originalCode}
                modified={modifiedCode}
                onMount={mounter}
            />
        );
    } else if (showNewDiffEditor) {
        return (
            <EditableCompare
                original={originalCode}
                modified={modifiedCode}
                onUpdateFunc={setModifiedCodeStr}
            />
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
