import { Helmet } from "react-helmet";
import React from "react";
import { Navigate, Route, Routes } from "react-router-dom";

import { ContentDirectory, getDirectoryElement } from "../MarkdownDirectory";

export interface IAMetaAndRouterProps {
    pageName: string;
    directories: ContentDirectory[];
    indexElement?: JSX.Element;
    redirectToIndex?: string;
}
export const IAMetaAndRouter = ({
    pageName,
    directories,
    indexElement,
    redirectToIndex,
}: IAMetaAndRouterProps) => {
    return (
        <>
            <Helmet>
                <title>
                    {pageName} | {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <Routes>
                {redirectToIndex && <Navigate to={redirectToIndex} />}
                {indexElement && <Route path={"/"} element={indexElement} />}
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
