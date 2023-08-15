import { Button, ButtonGroup, Link } from "@trussworks/react-uswds";
import React from "react";

export default {
    title: "Components/Button group",
    component: ButtonGroup,
};

export const Default = (): React.ReactElement => (
    <ButtonGroup type="default">
        <Link href="#" className="usa-button usa-button--outline">
            Back
        </Link>
        <Button type="button">Continue</Button>
    </ButtonGroup>
);

export const Segmented = (): React.ReactElement => (
    <ButtonGroup type="segmented">
        <Button type="button">Map</Button>
        <Button type="button" outline>
            Satellite
        </Button>
        <Button type="button" outline>
            Hybrid
        </Button>
    </ButtonGroup>
);
