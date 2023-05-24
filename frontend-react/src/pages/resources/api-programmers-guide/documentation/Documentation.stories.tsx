import { ComponentMeta, ComponentStoryObj } from "@storybook/react";

import DocumentationPage from "./Documentation";

export default {
    title: "pages/resources/documentation/Documentation",
    component: DocumentationPage,
} as ComponentMeta<typeof DocumentationPage>;

export const Default: ComponentStoryObj<typeof DocumentationPage> = {};
