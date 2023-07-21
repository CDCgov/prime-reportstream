import { mswDecorator } from "msw-storybook-addon";
import { CacheProvider } from "rest-hooks";
import { BrowserRouter } from "react-router-dom";
import { HelmetProvider } from "react-helmet-async";
import MarkdownLayout from "../src/layouts/Markdown/MarkdownLayout";
import MockDate from "mockdate";
import { DocsContainer, Unstyled } from "@storybook/blocks";
import { Meta } from "@storybook/addon-docs";

import "../src/global.scss";
import React from "react";

// mock all dates in stories to make sure we don't run into date-related inconsistencies
MockDate.set("2023-01-01");

function withRestHooksCacheProvider(Story) {
    return (
        <CacheProvider>
            <Story />
        </CacheProvider>
    );
}

function withRouter(Story) {
    return (
        <BrowserRouter>
            <Story />
        </BrowserRouter>
    );
}

function withHelmet(Story) {
    return (
        <HelmetProvider>
            <Story />
        </HelmetProvider>
    );
}

export const decorators = [
    withHelmet,
    withRouter,
    withRestHooksCacheProvider,
    mswDecorator,
];

const MyDocsContainer = (props) => {
    const isContent =
        props.context.channel.data.docsRendered?.[0].startsWith("content");
    return (
        <HelmetProvider>
            <BrowserRouter>
                <MarkdownLayout>
                    {isContent ? (
                        <Unstyled>
                            <DocsContainer {...props} />
                        </Unstyled>
                    ) : (
                        <DocsContainer {...props} />
                    )}
                </MarkdownLayout>
            </BrowserRouter>
        </HelmetProvider>
    );
};

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
    docs: {
        container: MyDocsContainer,
    },
};
