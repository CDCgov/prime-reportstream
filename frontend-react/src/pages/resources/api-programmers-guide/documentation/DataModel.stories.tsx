import { ComponentMeta, ComponentStoryObj } from "@storybook/react";

import DataModelPage from "./DataModel";

export default {
    title: "pages/resources/documentation/DataModel",
    component: DataModelPage,
} as ComponentMeta<typeof DataModelPage>;

export const Default: ComponentStoryObj<typeof DataModelPage> = {};
