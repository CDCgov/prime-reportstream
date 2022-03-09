import React, { FC } from "react";
import { DiffEditor } from "@monaco-editor/react";

import { CheckFeatureFlag } from "../../pages/misc/FeatureFlags";

export const DiffEditorComponent: FC<{
    originalCode: string;
    modifiedCode: string;
    language: string;
    mounter: (editor: null, monaco: any) => void;
}> = ({ modifiedCode, originalCode, language, mounter }) => {
    const showDiffEditor = CheckFeatureFlag("showDiffEditor");
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
    } else {
        return (
            <div>
                The diff editor is currently under maintenance. Please click
                Save below to save your changes
            </div>
        );
    }
};
