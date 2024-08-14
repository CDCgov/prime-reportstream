// AutoUpdateFileChromatic
import { Meta, StoryObj } from "@storybook/react";
import { Button } from "@trussworks/react-uswds";
import { ComponentProps, ReactElement } from "react";

import { AlertSimple } from "./Alert";

const testText = (
    <>
        Lorem ipsum dolor sit amet, <a href="#test">consectetur adipiscing</a>{" "}
        elit, sed do eiusmod.
    </>
);
const types = ["success", "warning", "error", "info"];

const AlertSimpleRender = ({
    type,
    ...props
}: ComponentProps<typeof AlertSimple>) => (
    <AlertSimple type={type} heading={`${type} status`} {...props} />
);

const AlertSimpleTypesRender = ({
    type: _,
    ...props
}: ComponentProps<typeof AlertSimple>) => (
    <>
        {types.map((t) => (
            <AlertSimpleRender
                key={t}
                type={t as ComponentProps<typeof AlertSimpleRender>["type"]}
                {...props}
            />
        ))}
    </>
);

export default {
    title: "components/Alert",
    component: AlertSimple,
    args: {
        children: testText,
    },
    argTypes: {
        children: {
            type: "string",
        },
        heading: {
            type: "string",
        },
    },
    render: AlertSimpleRender,
} as Meta<typeof AlertSimple>;

export const Info: StoryObj<typeof AlertSimple> = {
    args: {
        type: "info",
    },
};

export const Error: StoryObj<typeof AlertSimple> = {
    args: {
        type: "error",
    },
};

export const Success: StoryObj<typeof AlertSimple> = {
    args: {
        type: "success",
    },
};

export const Tip: StoryObj<typeof AlertSimple> = {
    args: {
        type: "tip",
    },
};

export const Warning: StoryObj<typeof AlertSimple> = {
    args: {
        type: "warning",
    },
};

export const Slim: StoryObj<typeof AlertSimple> = {
    args: {
        slim: true,
    },
    render: AlertSimpleTypesRender,
};
export const NoIcon: StoryObj<typeof AlertSimple> = {
    args: {
        noIcon: true,
    },
    render: AlertSimpleTypesRender,
};

export const HeadingLevels: StoryObj<typeof AlertSimple> = {
    args: {
        type: "info",
        heading: "Heading level 2",
        headingLevel: "h2",
    },
    render: (props): ReactElement => (
        <>
            <h1>Heading Level 1</h1>
            <AlertSimpleRender {...props} />
        </>
    ),
};
export const WithCTA: StoryObj<typeof AlertSimple> = {
    args: {
        type: "warning",
        cta: (
            <Button type="button" outline>
                Click here
            </Button>
        ),
    },
};

export const WithValidation: StoryObj<typeof AlertSimple> = {
    args: {
        type: "info",
        heading: "Code requirements",
        children: (
            <ul>
                <li>Use at least one uppercase character</li>
                <li>Use at least one number</li>
            </ul>
        ),
    },
};
