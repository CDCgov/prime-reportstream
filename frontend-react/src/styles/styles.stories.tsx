import React from "react";
import { ComponentMeta, ComponentStory } from "@storybook/react";

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
} as ComponentMeta<typeof Dummy>;

export const H4: ComponentStory<typeof Dummy> = (args) => (
    <h4>
        <Dummy {...args} />
    </h4>
);

export const BodySmall: ComponentStory<typeof Dummy> = (args) => (
    <div className="font-body-sm">
        <Dummy {...args} />
    </div>
);
