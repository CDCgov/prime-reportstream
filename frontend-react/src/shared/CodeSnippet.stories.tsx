import React from "react";
import {
    ComponentMeta,
    ComponentStory,
    ComponentStoryObj,
} from "@storybook/react";

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
    <section>
        Block variant: <CodeSnippet {...args} />
    </section>
);

Block.args = {
    children: blockText,
    highlightText: "healthy-labs",
    isBlock: true,
};

const diagramText = `bootstrap/
├── css/
│   ├── bootstrap.css
│   ├── bootstrap.min.css
│   ├── bootstrap-theme.css
│   └── bootstrap-theme.min.css
├── js/
│   ├── bootstrap.js
│   └── bootstrap.min.js
└── fonts/
    ├── glyphicons-halflings-regular.eot
    ├── glyphicons-halflings-regular.svg
    ├── glyphicons-halflings-regular.ttf
    └── glyphicons-halflings-regular.woff

bootstrap/
├── less/
├── js/
├── fonts/
├── dist/
│   ├── css/
│   ├── js/
│   └── fonts/
└── docs/
    └── examples/
`;

export const Diagram: ComponentStoryObj<typeof CodeSnippet> = {};

Diagram.args = {
    children: diagramText,
    figure: {
        role: "img",
        label: "Directory diagram",
        figureCaptionProps: {
            children: "Diagram depicting directory structure",
        },
    },
};
