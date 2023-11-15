import React from "react";
import { ComponentMeta, ComponentStory } from "@storybook/react";

import { CodeSnippet } from "./CodeSnippet";

export default {
    title: "components/CodeSnippet",
    component: CodeSnippet,
} as ComponentMeta<typeof CodeSnippet>;

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

export const Default: ComponentStory<typeof CodeSnippet> = () => (
    <p>
        <CodeSnippet
            copyString={`This is a code snippet to copy with highlighted kid healthy-labs`}
        >
            This is a code snippet to copy with highlighted kid healthy-labs
        </CodeSnippet>

        <CodeSnippet copyString={`./healthy-labs-nonPII-data.csv`}>
            ./healthy-labs-nonPII-data.csv
        </CodeSnippet>
        <CodeSnippet copyString={blockText}>{blockText}</CodeSnippet>
    </p>
);
