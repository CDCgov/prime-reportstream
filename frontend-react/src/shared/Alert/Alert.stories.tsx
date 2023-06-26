import { Meta, StoryObj } from "@storybook/react";

import { AlertSimple } from "./Alert";

export default {
    title: "components/Alert/Alert",
    component: AlertSimple,
    argTypes: {
        children: {
            type: "string",
        },
        heading: {
            type: "string",
        },
    },
    args: {
        type: "info",
    },
} as Meta<typeof AlertSimple>;

export const Default: StoryObj<typeof AlertSimple> = {};
