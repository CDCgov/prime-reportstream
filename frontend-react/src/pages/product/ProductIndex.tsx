import { Helmet } from "react-helmet";
import { Redirect, Switch } from "react-router-dom";
import React from "react";

import StaticPagesFromDirectories from "../../components/Content/StaticPagesFromDirectories";
import {
    productDirectories,
    ProductDirectoryTools,
} from "../../content/product";

export const Product = () => {
    return (
        <>
            <Helmet>
                <title>Product | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <div className="rs-hero__index">
                <div className="grid-container">
                    <h1>{ProductDirectoryTools.title}</h1>
                    <h2>{ProductDirectoryTools.subtitle}</h2>
                </div>
            </div>
            <Switch>
                {/* Workaround to allow links to /product to work -- means I can't use
                 IAMetaAndRouter to do this. Sad face. */}
                <Redirect
                    from={"/product"}
                    to={"/product/overview"}
                    exact={true}
                />
            </Switch>
            <div>
                <StaticPagesFromDirectories directories={productDirectories} />
            </div>
        </>
    );
};
