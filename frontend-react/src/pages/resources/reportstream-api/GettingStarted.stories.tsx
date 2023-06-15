import { ComponentMeta, ComponentStoryObj } from "@storybook/react";

import MarkdownLayout from "../../../components/Content/MarkdownLayout";
import Method1 from "../../../content/resources/reportstream-api/getting-started/Method1.mdx";
import Method2 from "../../../content/resources/reportstream-api/getting-started/Method2.mdx";

import { GettingStartedPage } from "./GettingStarted";

export default {
    title: "pages/resources/GettingStarted",
    component: GettingStartedPage,
} as ComponentMeta<typeof GettingStartedPage>;

export const Default: ComponentStoryObj<typeof GettingStartedPage> = {};

export const Method1Section: ComponentStoryObj<typeof Method1> = {
    render: () => (
        <MarkdownLayout sidenav={<></>}>
            <Method1 />
        </MarkdownLayout>
    ),
};

export const Method2Section: ComponentStoryObj<typeof Method2> = {
    render: () => (
        <MarkdownLayout sidenav={<></>}>
            <Method2 />
        </MarkdownLayout>
    ),
};
