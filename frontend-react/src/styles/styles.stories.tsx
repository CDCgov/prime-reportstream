import React from "react";
import { StoryObj, Meta } from "@storybook/react";

function Dummy({ children }: { children: React.ReactNode }) {
    return <>{children}</>;
}

export default {
    title: "styles/Unsorted",
    component: Dummy,
    argTypes: {
        children: {
            defaultValue: "Lorum ipsum",
        },
    },
    args: {
        children: "Lorum ipsum",
    },
} as Meta<typeof Dummy>;

export const H4: StoryObj<typeof Dummy> = {
    render: (args) => (
        <h4>
            <Dummy {...args} />
        </h4>
    ),
};

export const BodySmall: StoryObj<typeof Dummy> = {
    render: (args) => (
        <div className="font-body-sm">
            <Dummy {...args} />
        </div>
    ),
};
