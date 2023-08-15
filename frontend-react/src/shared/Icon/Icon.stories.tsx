// AutoUpdateFileChromatic
import { Meta, StoryObj } from "@storybook/react";

import Icon from "./Icon";

export default {
    title: "components/Icon",
    component: Icon,
} as Meta<typeof Icon>;

export const Default: StoryObj<typeof Icon> = {
    args: {
        name: "CheckCircleOutline",
    },
};
