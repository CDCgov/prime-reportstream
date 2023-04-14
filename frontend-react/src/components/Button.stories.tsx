import type { ComponentMeta, ComponentStoryObj } from "@storybook/react";

import { UnnestedButton as Button } from "./Button";

const meta: ComponentMeta<typeof Button> = {
    title: "components/Button",
    component: Button,
    argTypes: {
        tooltip__tooltipContentProps__children: {
            table: {
                category: "tooltip__tooltipContentProps",
            },
        },
    },
    args: {
        children: "Button",
    },
};

export default meta;
type Story = ComponentStoryObj<typeof Button>;

export const Default: Story = {};

export const Tooltip: Story = {
    args: {
        tooltip__tooltipContentProps__children: "Hello",
        tooltip__tooltipProps__open: true,
    },
};
