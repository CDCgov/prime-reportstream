// AutoUpdateFileChromatic
import { StoryObj, Meta } from "@storybook/react";

import SiteLink from "./SiteLink";

export default {
    title: "Components/SiteLink",
    component: SiteLink,
} as Meta<typeof SiteLink>;

export const Default: StoryObj<typeof SiteLink> = {
    args: {
        children: "Site link",
    },
};
