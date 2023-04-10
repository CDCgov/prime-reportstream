import React from "react";
import { ComponentMeta, ComponentStory } from "@storybook/react";

import { USExtLink, USLink, USLinkButton } from "./USLink";

export default {
    title: "components/USLink",
    component: USLink,
} as ComponentMeta<typeof USLink>;

export const Default: ComponentStory<typeof USLink> = (args) => (
    <USLink {...args} />
);
Default.args = {
    className: "",
    children: "This is a link",
};

export const Button: ComponentStory<typeof USLinkButton> = (args) => (
    <USLinkButton {...args} />
);
Button.args = {
    children: "This is a link styled as a button",
    secondary: false,
    base: false,
    inverse: false,
    unstyled: false,
};
Button.argTypes = {
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
};

export const ExtLink: ComponentStory<typeof USExtLink> = (args) => (
    <USExtLink {...args} />
);
ExtLink.args = {
    className: "",
    children: "This is an external link",
};
