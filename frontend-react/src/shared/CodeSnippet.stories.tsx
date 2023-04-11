import React from "react";
import { ComponentMeta, ComponentStory } from "@storybook/react";

import { CodeSnippet } from "./CodeSnippet";

export default {
    title: "components/CodeSnippet",
    component: CodeSnippet,
    argTypes: {
        children: {
            type: "string",
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
    <p>
        Lorem ipsum <CodeSnippet {...args} />
    </p>
);

export const Block: ComponentStory<typeof CodeSnippet> = (args) => (
    <CodeSnippet {...args} isBlock={true} />
);
