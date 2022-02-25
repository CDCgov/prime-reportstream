import React, { FC } from "react";
import { DiffEditor } from "@monaco-editor/react";

export const DiffEditorComponent: FC<{
    originalCode: string;
    modifiedCode: string;
    language: string;
    mounter: (editor: null, monaco: any) => void;
}> = ({ modifiedCode, originalCode, language, mounter }) => {
    // const diffEditorRef = useRef(null);
    //
    // function handleEditorDidMount(editor: null, monaco: any) {
    //     diffEditorRef.current = editor;
    // }

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
};
