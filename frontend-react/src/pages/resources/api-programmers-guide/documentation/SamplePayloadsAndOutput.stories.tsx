import { ComponentMeta, ComponentStoryObj } from "@storybook/react";

import SamplePayloadsAndOutput from "./SamplePayloadsAndOutput";

export default {
    title: "pages/resources/documentation/SamplePayloadsAndOutput",
    component: SamplePayloadsAndOutput,
} as ComponentMeta<typeof SamplePayloadsAndOutput>;

export const Default: ComponentStoryObj<typeof SamplePayloadsAndOutput> = {};
