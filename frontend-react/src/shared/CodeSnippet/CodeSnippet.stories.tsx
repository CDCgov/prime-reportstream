import { StoryFn, StoryObj } from "@storybook/react";

import CodeSnippet from "./CodeSnippet";

export default {
    title: "components/CodeSnippet",
    component: CodeSnippet,
} as StoryObj<typeof CodeSnippet>;

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

export const Default: StoryFn<typeof CodeSnippet> = () => (
    <p>
        <CodeSnippet>
            This is a code snippet to copy with highlighted kid healthy-labs
        </CodeSnippet>

        <CodeSnippet>./healthy-labs-nonPII-data.csv</CodeSnippet>
        <CodeSnippet>{blockText}</CodeSnippet>
    </p>
);
