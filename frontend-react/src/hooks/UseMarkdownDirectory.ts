import { useEffect, useMemo, useReducer } from "react";

export interface UseMarkdownInit {
    fromDir: string;
    files: string[];
}

interface MdDirAction {
    type: "ADD";
    payload: string;
}

const mdFilesReducer = (state: string[], action: MdDirAction) => {
    const { type, payload } = action;
    switch (type) {
        case "ADD":
            return [...state, payload];
    }
};

const useMarkdownDirectory = ({ fromDir, files }: UseMarkdownInit) => {
    /* Using reducer pattern to prevent effect dependency on state */
    const [mdFiles, setMdFiles] = useReducer(mdFilesReducer, []);
    // Using memo for a stable reference since this is a useEffect dep
    const mdUrls = useMemo(() => {
        return files.map((fileName) => `${fromDir}/${fileName}`);
    }, [files, fromDir]);

    useEffect(() => {
        let subscribed = true;
        mdUrls.map(async (url) => {
            // Stops from trying to set state when component tears down during
            // async fetch
            if (!subscribed) return;
            fetch(url)
                .then((response) => response.text())
                .then((text) => {
                    setMdFiles({
                        type: "ADD",
                        payload: text,
                    });
                });
        });
        return () => {
            subscribed = false;
        };
    }, [mdUrls]);

    return {
        mdFiles,
    };
};

export default useMarkdownDirectory;
