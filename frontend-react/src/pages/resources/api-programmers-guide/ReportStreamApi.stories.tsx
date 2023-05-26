import { ComponentMeta, ComponentStoryObj } from "@storybook/react";

import { ReportStreamAPIPage } from "./ReportStreamApi";

export default {
    title: "pages/resources/ReportStreamAPI",
    component: ReportStreamAPIPage,
} as ComponentMeta<typeof ReportStreamAPIPage>;

export const Default: ComponentStoryObj<typeof ReportStreamAPIPage> = {};
