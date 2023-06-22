import { StoryObj, Meta } from "@storybook/react";

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
} as Meta<typeof StaticAlert>;

export const Default: StoryObj<typeof StaticAlert> = {
    args: {
        type: StaticAlertType.Success,
        heading: "StaticAlert Heading",
        message: "This is the message of the StaticAlert",
        children: "Children are optional! (Try clearing me out)",
    },
};
