import React from "react";
import { ComponentMeta, ComponentStory } from "@storybook/react";

import { CodeSnippet } from "./CodeSnippet";

export default {
    title: "components/CodeSnippet",
    component: CodeSnippet,
    argTypes: {
        children: {
            defaultValue:
                "openssl rsa -in my-rsa-keypair.pem -outform PEM -pubout -out my-rsa-public-key.pem",
        },
        onButtonClick: {
            defaultValue: undefined,
        },
    },
    args: {
        onButtonClick: undefined,
    },
} as ComponentMeta<typeof CodeSnippet>;

export const Default: ComponentStory<typeof CodeSnippet> = (args) => (
    <CodeSnippet {...args} />
);

export const Narrow: ComponentStory<typeof CodeSnippet> = (args) => (
    <div style={{ width: "500px" }}>
        <CodeSnippet {...args} />
    </div>
);
