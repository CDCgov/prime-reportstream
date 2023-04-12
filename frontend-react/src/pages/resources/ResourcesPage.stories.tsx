import { ComponentMeta, ComponentStoryObj } from "@storybook/react";

import { ResourcesPage } from "./ResourcesPage";

export default {
    title: "pages/resources/Resources",
    component: ResourcesPage,
} as ComponentMeta<typeof ResourcesPage>;

export const Default: ComponentStoryObj<typeof ResourcesPage> = {};
