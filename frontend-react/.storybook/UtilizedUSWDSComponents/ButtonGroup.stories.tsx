import { Button, ButtonGroup, Link } from "@trussworks/react-uswds";
import React from "react";

export default {
    title: "Components/Button groups",
    component: ButtonGroup,
    parameters: {
        docs: {
            description: {
                component: `
### USWDS 3.0 ButtonGroup component

Source: https://designsystem.digital.gov/components/button-groups/
`,
            },
        },
    },
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
