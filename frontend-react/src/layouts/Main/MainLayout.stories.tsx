// AutoUpdateFileChromatic
import React from "react";
import { withRouter } from "storybook-addon-react-router-v6";

import SessionProvider from "../../contexts/SessionContext";

import { MainLayoutBase } from "./MainLayout";

export default {
    title: "components/MainLayout",
    component: MainLayoutBase,
    decorators: [withRouter],
};

const Filler = () => (
    <>
        <h1>Page Title</h1>

        <p>
            Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
            eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim
            ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut
            aliquip ex ea commodo consequat. Duis aute irure dolor in
            reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
            pariatur. Excepteur sint occaecat cupidatat non proident, sunt in
            culpa qui officia deserunt mollit anim id est laborum.
        </p>
    </>
);
const RouteComponent = () => (
    <SessionProvider>
        <MainLayoutBase />
    </SessionProvider>
);

export const Default = {
    parameters: {
        reactRouter: {
            routing: {
                children: [{ path: "/app" }],
                Component: RouteComponent,
            },
            path: "/app",
        },
    },
    render: () => <Filler />,
};

export const Content = {
    parameters: {
        reactRouter: {
            routing: {
                children: [
                    {
                        path: "/content",
                        handle: {
                            isContentPage: true,
                        },
                    },
                ],
                Component: RouteComponent,
            },
            path: "/content",
        },
    },
    render: () => (
        <article className="tablet:grid-col-12">
            <Filler />
        </article>
    ),
};

export const FullWidth = {
    parameters: {
        reactRouter: {
            routing: {
                children: [
                    {
                        path: "/full-width",
                        handle: {
                            isFullWidth: true,
                        },
                    },
                ],
                Component: RouteComponent,
            },
            path: "/full-width",
        },
    },
    render: () => <Filler />,
};
