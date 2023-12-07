import { Grid } from "@trussworks/react-uswds";
import { PropsWithChildren } from "react";

import "./SettingFormField.scss";

export interface SettingFormFieldRowProps extends PropsWithChildren {
    label?: JSX.Element;
}

export function SettingFormFieldRow({
    label,
    children,
}: SettingFormFieldRowProps) {
    return (
        <Grid row>
            {label && <Grid col={3}>{label}</Grid>}
            <Grid col={label ? 9 : 12}>{children}</Grid>
        </Grid>
    );
}
