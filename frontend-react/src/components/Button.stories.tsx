import type { ComponentMeta, ComponentStoryObj } from "@storybook/react";

//import { unflattenProps } from "../utils/misc";

import { Button } from "./Button";

const meta: ComponentMeta<typeof Button> = {
    title: "components/IconButton",
    component: Button,
};

export default meta;
type Story = ComponentStoryObj<typeof Button>;

export const Default: Story = {};

// DEFAULT STORY
// TOOLTIP STORY
