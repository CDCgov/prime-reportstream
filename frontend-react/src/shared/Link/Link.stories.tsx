// AutoUpdateFileChromatic
import React from "react";
import { StoryObj, Meta } from "@storybook/react";

import { Link } from "./Link";

export default {
    title: "Components/Link",
    component: Link,
} as Meta<typeof Link>;

export const Default: StoryObj<typeof Link> = {
    args: {
        className: "",
        children: "This is a link",
    },
};

export const Button: StoryObj<typeof Link> = {
    render: (args) => <Link {...args} />,

    args: {
        children: "This is a link styled as a button",
        button: {
            secondary: false,
            base: false,
            inverse: false,
            unstyled: false,
        },
    },

    argTypes: {
        button: {
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
    },
};

export const ExtLink: StoryObj<typeof Link> = {
    render: (args) => <Link {...args} />,

    args: {
        className: "",
        children: "This is an external link",
    },
};
