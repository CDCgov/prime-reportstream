import { ComponentMeta, ComponentStoryObj } from "@storybook/react";

import { Tooltip, TOOLTIP_POSITIONS } from "./Tooltip";

export default {
    title: "components/Tooltip",
    component: Tooltip,
    argTypes: {
        position: {
            type: "string",
            control: "select",
            options: TOOLTIP_POSITIONS,
        },
    },
    args: {
        children: "Lorem ipsum",
        isSet: true,
        isVisible: true,
    },
} as ComponentMeta<typeof Tooltip>;

export const Default: ComponentStoryObj<typeof Tooltip> = {};
