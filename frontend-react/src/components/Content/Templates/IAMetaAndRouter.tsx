import { Helmet } from "react-helmet";
import React from "react";
import { Route, Routes } from "react-router-dom";

import { ContentDirectory, getDirectoryElement } from "../MarkdownDirectory";

export interface IAMetaAndRouterProps {
    pageName: string;
    directories: ContentDirectory[];
}
export const IAMetaAndRouter = ({
    directories,
    pageName,
}: IAMetaAndRouterProps) => {
    return (
        <>
            <Helmet>
                <title>
                    {pageName} | {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>

            <Routes>
                {directories.map((dir, idx) => (
                    <Route
                        key={idx}
                        path={dir.slug}
                        element={getDirectoryElement(dir)}
                    />
                ))}
            </Routes>
        </>
    );
};
