import type { ComponentMeta, ComponentStoryObj } from "@storybook/react";

import { UnnestedIconButton as IconButton } from "./IconButton";

const meta: ComponentMeta<typeof IconButton> = {
    title: "components/IconButton",
    component: IconButton,
    argTypes: {
        iconProps__icon: {
            table: {
                category: "iconProps",
            },
        },
        tooltip__tooltipContentProps__children: {
            table: {
                category: "tooltipProps__tooltipContentProps",
            },
        },
    },
    args: {
        iconProps__icon: "Construction",
    },
};

export default meta;
type Story = ComponentStoryObj<typeof IconButton>;

export const Default: Story = {};

export const Tooltip: Story = {
    args: {
        tooltip__tooltipContentProps__children: "Hello",
    },
};
