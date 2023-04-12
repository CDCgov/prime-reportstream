import React from "react";
import { ComponentMeta, ComponentStory } from "@storybook/react";

import { StaticAlert, StaticAlertType } from "./StaticAlert";

export default {
    title: "components/StaticAlert",
    component: StaticAlert,
    argTypes: {
        type: {
            control: {
                type: "radio",
                options: Object.values(StaticAlertType),
            },
        },
    },
} as ComponentMeta<typeof StaticAlert>;

export const Default: ComponentStory<typeof StaticAlert> = (args) => (
    <StaticAlert {...args} />
);
Default.args = {
    type: StaticAlertType.Success,
    heading: "StaticAlert Heading",
    message: "This is the message of the StaticAlert",
    children: "Children are optional! (Try clearing me out)",
};
