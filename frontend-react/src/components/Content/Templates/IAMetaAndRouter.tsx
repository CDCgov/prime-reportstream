import { Helmet } from "react-helmet";
import { Route, Routes } from "react-router-dom";
import React from "react";

import { ContentDirectory, getDirectoryElement } from "../MarkdownDirectory";

export interface IAMetaAndRouterProps {
    path: string; // include preceding slash
    pageName: string;
    indexComponent: React.ComponentType<any>; // type required by Route.component prop
    directoriesToRoute: ContentDirectory[];
}
export const IAMetaAndRouter = ({
    path,
    pageName,
    indexComponent,
    directoriesToRoute,
}: IAMetaAndRouterProps) => {
    return (
        <>
            <Helmet>
                <title>
                    {pageName} | {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>

            <Routes>
                <Route path={path} element={indexComponent} />
                {directoriesToRoute.map((dir, idx) => (
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
