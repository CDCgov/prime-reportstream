import { ComponentMeta, ComponentStoryObj } from "@storybook/react";

import {
    Tooltip,
    TooltipContent,
    TooltipTrigger,
    TOOLTIP_POSITIONS,
} from "./Tooltip";

export default {
    title: "components/Tooltip",
    component: Tooltip,
    argTypes: {
        placement: {
            type: "string",
            control: "select",
            options: TOOLTIP_POSITIONS,
        },
        triggerContent: {
            type: "string",
        },
        content: {
            type: "string",
        },
    },
    args: {
        content: "Lorem ipsum",
        triggerContent: "Hover me!",
        placement: "top",
        onOpenChange: undefined,
    },
    render: ({ content, triggerContent, ...args }: any) => (
        <Tooltip {...args}>
            <TooltipTrigger>{triggerContent}</TooltipTrigger>
            <TooltipContent>{content}</TooltipContent>
        </Tooltip>
    ),
} as ComponentMeta<typeof Tooltip>;

export const Default: ComponentStoryObj<typeof Tooltip> = {};
