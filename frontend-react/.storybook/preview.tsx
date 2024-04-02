import { initialize, mswLoader } from "msw-storybook-addon";
import { CacheProvider } from "rest-hooks";
import { HelmetProvider } from "react-helmet-async";
import MockDate from "mockdate";

import "../src/global.scss";
import React from "react";

// mock all dates in stories to make sure we don't run into date-related inconsistencies
MockDate.set("2023-01-01");

export const parameters = {
    actions: { argTypesRegex: "^on[A-Z].*" },
    controls: {
        matchers: {
            color: /(background|color)$/i,
            date: /Date$/,
        },
    },
    options: {
        storySort: {
            method: "alphabetical",
            locales: "en-US",
        },
    },
};

initialize();

function withRestHooksCacheProvider(Story) {
    return (
        <CacheProvider>
            <Story />
        </CacheProvider>
    );
}

function withHelmet(Story) {
    return (
        <HelmetProvider>
            <Story />
        </HelmetProvider>
    );
}

export const decorators = [withHelmet, withRestHooksCacheProvider];

export const loaders = [mswLoader];
