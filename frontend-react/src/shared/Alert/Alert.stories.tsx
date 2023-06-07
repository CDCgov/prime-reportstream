import { ComponentMeta, ComponentStoryObj } from "@storybook/react";

import Alert from "./Alert";

export default {
    title: "components/Alert",
    component: Alert,
} as ComponentMeta<typeof Alert>;

export const Default: ComponentStoryObj<typeof Alert> = {};
