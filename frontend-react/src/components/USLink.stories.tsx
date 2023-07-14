import React from "react";
import { StoryObj, Meta } from "@storybook/react";

import { USExtLink, USLink, USLinkButton } from "./USLink";

export default {
    title: "Components/USLink",
    component: USLink,
} as Meta<typeof USLink>;

export const Default: StoryObj<typeof USLink> = {
    args: {
        className: "",
        children: "This is a link",
    },
};

export const Button: StoryObj<typeof USLinkButton> = {
    render: (args) => <USLinkButton {...args} />,

    args: {
        children: "This is a link styled as a button",
        secondary: false,
        base: false,
        inverse: false,
        unstyled: false,
    },

    argTypes: {
        accentStyle: {
            control: {
                type: "radio",
                options: ["", "cool", "warm"],
            },
        },
        size: {
            control: {
                type: "radio",
                options: ["", "big"],
            },
        },
    },
};

export const ExtLink: StoryObj<typeof USExtLink> = {
    render: (args) => <USExtLink {...args} />,

    args: {
        className: "",
        children: "This is an external link",
    },
};
