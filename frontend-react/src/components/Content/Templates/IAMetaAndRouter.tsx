import { Helmet } from "react-helmet";
import { Route, Switch } from "react-router-dom";
import React from "react";

import { ContentDirectory, GeneratedRouter } from "../MarkdownDirectory";

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

            <Switch>
                <Route exact path={path} component={indexComponent} />
                <GeneratedRouter directories={directoriesToRoute} />
            </Switch>
        </>
    );
};
