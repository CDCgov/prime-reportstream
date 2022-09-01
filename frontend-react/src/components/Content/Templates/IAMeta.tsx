import { Helmet } from "react-helmet";
import React from "react";
import { Route, Routes } from "react-router-dom";

import { ContentDirectory, getDirectoryElement } from "../MarkdownDirectory";

export interface IARouterProps {
    directories: ContentDirectory[];
    indexElement?: JSX.Element;
}
export const IARouter = ({ directories, indexElement }: IARouterProps) => {
    return (
        <Routes>
            {indexElement && <Route path={"/"} element={indexElement} />}
            {directories.map((dir, idx) => (
                <Route
                    key={idx}
                    path={dir.slug}
                    element={getDirectoryElement(dir)}
                />
            ))}
        </Routes>
    );
};

export interface IAMetaProps {
    pageName: string;
}
export const IAMeta = ({ pageName }: IAMetaProps) => {
    return (
        <>
            <Helmet>
                <title>
                    {pageName} | {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
        </>
    );
};
