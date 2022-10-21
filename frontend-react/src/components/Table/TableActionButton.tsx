/* this button will be placed above the rendered table and on `click` will run an arbitrary function
 * passed in from the Table's parent, or an addRow function defined by the Table.
 * in order to avoid problems around timing, takes a `disabled` prop. */
import { Button } from "@trussworks/react-uswds";
import React from "react";

import { DatasetAction } from "./TableInfo";

type TableActionButtonProps = DatasetAction & {
    disabled: boolean;
};
export const TableActionButton = ({
    label,
    method = () => {},
    disabled,
}: TableActionButtonProps) => (
    <Button type={"button"} onClick={() => method()} disabled={disabled}>
        {label}
    </Button>
);
