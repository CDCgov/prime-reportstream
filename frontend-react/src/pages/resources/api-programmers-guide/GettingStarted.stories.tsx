import { ComponentMeta, ComponentStoryObj } from "@storybook/react";

import Method1 from "../../../content/resources/api-programmers-guide/getting-started/Method1.mdx";

import { GettingStartedPage } from "./GettingStarted";

export default {
    title: "pages/resources/ReportStreamAPI",
    component: GettingStartedPage,
} as ComponentMeta<typeof GettingStartedPage>;

export const Default: ComponentStoryObj<typeof GettingStartedPage> = {};

export const Method1Section: ComponentStoryObj<typeof Method1> = {
    render: () => <Method1 />,
};
