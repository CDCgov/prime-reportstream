import { Meta, StoryObj } from "@storybook/react";

import { AlertIconSimple } from "./AlertIcon";

export default {
    title: "components/Alert/AlertIcon",
    component: AlertIconSimple,
} as Meta<typeof AlertIconSimple>;

export const Info: StoryObj<typeof AlertIconSimple> = {
    args: {
        type: "info",
    },
};

export const Error: StoryObj<typeof AlertIconSimple> = {
    args: {
        type: "error",
    },
};

export const Warning: StoryObj<typeof AlertIconSimple> = {
    args: {
        type: "warning",
    },
};

export const Success: StoryObj<typeof AlertIconSimple> = {
    args: {
        type: "success",
    },
};

export const Tip: StoryObj<typeof AlertIconSimple> = {
    args: {
        type: "tip",
    },
};
