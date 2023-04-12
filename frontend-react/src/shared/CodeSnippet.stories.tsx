import React from "react";
import { ComponentMeta, ComponentStory } from "@storybook/react";

import { CodeSnippet } from "./CodeSnippet";

export default {
    title: "components/CodeSnippet",
    component: CodeSnippet,
    args: {
        onButtonClick: undefined,
    },
} as ComponentMeta<typeof CodeSnippet>;

export const Default: ComponentStory<typeof CodeSnippet> = (args) => (
    <p>
        The examples provided use the fake client-id{" "}
        <CodeSnippet {...args}>healthy-labs</CodeSnippet>
        that you will change for your submissions. The examples submit the
        payload contained in the file{" "}
        <CodeSnippet {...args}>./healthy-labs-nonPII-data.csv</CodeSnippet>
        (or .hl7).
    </p>
);

const blockText = `{
    "header": {
        "kid": "healthy-labs.default",
        "typ": "JWT",
        "alg": "RS256"
    },
    "payload": {
        "iss": "healthy-labs.default",
        "sub": "healthy-labs.default",
        "aud": "staging.prime.cdc.gov",
        "exp": 1660737164,
        "jti": "4b713fcd-2514-4207-b310-620b95b749c5"
    }
}`;

export const Block: ComponentStory<typeof CodeSnippet> = (args) => (
    <p>
        Block variant: <CodeSnippet {...args} />
    </p>
);

Block.args = {
    children: blockText,
    highlightText: "healthy-labs",
    isBlock: true,
};
