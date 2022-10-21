import React, { ReactNode } from "react";

import { TableActionButton } from "./TableActionButton";

export interface DatasetAction {
    label: string;
    method?: Function;
}

interface TableInfoProps {
    // Left non-optional because we should always be passing this into DatasetActionButton
    rowToEdit: number | undefined;
    // All optional since they conditionally render
    title?: string;
    legend?: ReactNode;
    datasetAction?: DatasetAction;
}
export const TableInfo = ({
    title,
    legend,
    datasetAction,
    rowToEdit,
}: TableInfoProps) => {
    return (
        <div className="grid-col-12 display-flex flex-align-end flex-justify-between">
            <div className="grid-col-8 display-flex flex-column">
                {title ? <h2>{title}</h2> : null}
                {legend ? legend : null}
            </div>
            <div className="grid-col-2 display-flex flex-column">
                {datasetAction ? (
                    <TableActionButton
                        label={datasetAction.label}
                        method={datasetAction.method}
                        disabled={!!rowToEdit}
                    />
                ) : null}
            </div>
        </div>
    );
};
