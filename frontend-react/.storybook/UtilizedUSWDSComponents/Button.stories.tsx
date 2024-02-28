import { Button } from "@trussworks/react-uswds";
import React, { ReactElement } from "react";

export default {
    title: "Components/Button",
    component: Button,
};

export const defaultButton = (): ReactElement => (
    <Button type="button">Click Me</Button>
);

export const secondary = (): ReactElement => (
    <Button type="button" secondary>
        Click Me
    </Button>
);

export const accentCool = (): ReactElement => (
    <Button type="button" accentStyle="cool">
        Click Me
    </Button>
);

export const accentWarm = (): ReactElement => (
    <Button type="button" accentStyle="warm">
        Click Me
    </Button>
);

export const base = (): ReactElement => (
    <Button type="button" base>
        Click Me
    </Button>
);

export const outline = (): ReactElement => (
    <Button type="button" outline>
        Click Me
    </Button>
);

export const inverse = (): ReactElement => (
    <Button type="button" inverse>
        Click Me
    </Button>
);

export const big = (): ReactElement => (
    <Button type="button" size="big">
        Click Me
    </Button>
);

export const unstyled = (): ReactElement => (
    <Button type="button" unstyled>
        Click Me
    </Button>
);

export const customClass = (): ReactElement => (
    <Button type="button" className="custom-class">
        Click Me
    </Button>
);

export const disabled = (): ReactElement => (
    <Button type="button" disabled>
        Click Me
    </Button>
);
