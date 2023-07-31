// AutoUpdateFileChromatic
import { Meta, StoryObj } from "@storybook/react";

import { AlertSimple } from "./Alert";

export default {
    title: "components/Alert/Alert",
    component: AlertSimple,
    args: {
        children: "Body",
        heading: "Heading",
    },
    argTypes: {
        children: {
            type: "string",
        },
        heading: {
            type: "string",
        },
    },
} as Meta<typeof AlertSimple>;

export const Info: StoryObj<typeof AlertSimple> = {
    args: {
        type: "info",
    },
};

export const Error: StoryObj<typeof AlertSimple> = {
    args: {
        type: "error",
    },
};

export const Success: StoryObj<typeof AlertSimple> = {
    args: {
        type: "success",
    },
};

export const Tip: StoryObj<typeof AlertSimple> = {
    args: {
        type: "tip",
    },
};

export const Warning: StoryObj<typeof AlertSimple> = {
    args: {
        type: "warning",
    },
};
