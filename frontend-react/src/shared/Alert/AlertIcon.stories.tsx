import { Meta, StoryObj } from "@storybook/react";

import { AlertIconSimple } from "./AlertIcon";

export default {
    title: "components/Alert/AlertIcon",
    component: AlertIconSimple,
    args: {
        type: "info",
    },
} as Meta<typeof AlertIconSimple>;

export const Default: StoryObj<typeof AlertIconSimple> = {};
