// AutoUpdateFileChromatic
import React from "react";

import HeroWrapper from "./HeroWrapper";

export default {
    title: "components/HeroWrapper",
    component: HeroWrapper,
};

const Content = () => (
    <header>
        <h1 className="margin-y-2">
            Your single connection to simplify data transfer and improve public
            health
        </h1>
        <p className="font-sans-lg margin-top-4">
            ReportStream is CDC’s free, interoperable platform for streamlining
            public health reporting. We navigate unique, complex requirements
            and work to make sure your data gets where it needs to be.
        </p>
        <div className="grid-row margin-top-8 margin-bottom-2">
            <a
                href="https://app.smartsheetgov.com/b/form/48f580abb9b440549b1a9cf996ba6957"
                className="usa-link usa-link--external usa-button"
                target="_blank"
                rel="noreferrer noopener"
            >
                Connect now
            </a>
        </div>
    </header>
);

export const Default = {
    args: {
        children: <Content />,
    },
};
export const Alternate = {
    args: {
        ...Default.args,
        isAlternate: true,
    },
};
