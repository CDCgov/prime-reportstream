import type { ComponentMeta, ComponentStoryObj } from "@storybook/react";
import { Icon } from "@trussworks/react-uswds";
import { JSXElementConstructor } from "react";

import { unflattenProps } from "../utils/misc";

import { IconButton } from "./IconButton";

type NonStrictMeta<C extends JSXElementConstructor<any>> = ComponentMeta<C> & {
    argTypes: { [k: string]: unknown };
    args: { [k: string]: unknown };
};

const meta: NonStrictMeta<typeof IconButton> = {
    title: "components/IconButton",
    component: IconButton,
    argTypes: {
        iconProps__icon: {
            type: "enum",
            options: Object.keys(Icon).filter((k) => k !== "prototype"),
            table: {
                category: "iconProps",
            },
        },
    },
    args: {
        iconProps__icon: "Construction",
    },
    render: (args) => <IconButton {...unflattenProps(args, ["iconProps"])} />,
};

export default meta;
type Story = ComponentStoryObj<typeof IconButton>;

export const Default: Story = {};
