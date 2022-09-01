import { Helmet } from "react-helmet";
import { useNavigate } from "react-router-dom";
import React, { useEffect } from "react";

import StaticPagesFromDirectories from "../../components/Content/StaticPagesFromDirectories";
import {
    productDirectories,
    ProductDirectoryTools,
} from "../../content/product";

export const Product = () => {
    const navigate = useNavigate();
    useEffect(() => navigate("/product/overview"));
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
            <div>
                <StaticPagesFromDirectories directories={productDirectories} />
            </div>
        </>
    );
};
