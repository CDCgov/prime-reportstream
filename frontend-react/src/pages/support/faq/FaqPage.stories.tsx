import { ComponentMeta, ComponentStoryObj } from "@storybook/react";

import { FaqPage } from "./FaqPage";

export default {
    title: "pages/support/FAQ",
    component: FaqPage,
} as ComponentMeta<typeof FaqPage>;

export const Default: ComponentStoryObj<typeof FaqPage> = {};
