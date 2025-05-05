import { Button } from "@trussworks/react-uswds";
import { ReactNode } from "react";

import { ActionableColumn, ColumnConfig, LinkableColumn } from "./Table";
import { TableRowData } from "./TableRows";
import { USLink } from "../USLink";

export interface ColumnProps {
    rowIndex: number;
    colIndex: number;
    rowData: TableRowData;
    columnConfig: ColumnConfig;
    editing?: boolean;
    setUpdatedRow?: (value: any, field: string) => void;
}

const showMappedFieldValue = (columnConfig: ColumnConfig, rowData: TableRowData) => {
    const rawFieldValue = rowData[columnConfig.dataAttr];
    if (columnConfig.valueMap) {
        return <span>{columnConfig.valueMap?.get(rawFieldValue) || rawFieldValue}</span>;
    } else {
        return <span>{rawFieldValue}</span>;
    }
};

// renders display for a single data field
export const ColumnData = ({
    rowIndex,
    colIndex,
    rowData,
    columnConfig,
    editing,
    setUpdatedRow = () => void 0,
}: ColumnProps) => {
    // Util functions
    // TODO: move these functions outside of the render

    // Easy-to-read way to transform value
    const transform = (transformFunc: (...args: any[]) => unknown, transformVal: string | number) => {
        return transformFunc(transformVal);
    };
    // Runtime type checking for ColumnFeature
    const hasFeature = (attr: string): boolean => {
        if (!columnConfig.feature) return false;
        return Object.keys(columnConfig.feature).includes(attr);
    };
    // Editing state indicator
    const isEditing = (): boolean => (editing && columnConfig.editable) ?? false;

    // <td> wrapper w/ key
    const tableData = (child: ReactNode) => <td key={`${rowIndex}:${colIndex}`}>{child}</td>;

    // indexes into rowData map based on the column's dataAttr value
    const field = columnConfig.dataAttr;
    let displayValue = rowData[field];

    if (columnConfig.transform) {
        displayValue = transform(columnConfig.transform, rowData[field]);
    }

    if (hasFeature("link")) {
        // Render column value as NavLink
        const feature = columnConfig?.feature as LinkableColumn;
        return tableData(
            <USLink
                href={`${feature.linkBasePath ?? ""}${rowData[feature.linkAttr ?? field]}`}
                state={feature.linkState || {}}
            >
                {columnConfig.valueMap ? showMappedFieldValue(columnConfig, rowData) : displayValue}
            </USLink>,
        );
    }

    if (hasFeature("action")) {
        // Make column value actionable
        const { action, param, actionButtonHandler, actionButtonParam } = columnConfig.feature as ActionableColumn;

        if (!param || !rowData[param]) {
            throw new Error(`The row attribute '${param}' could not be found`);
        }

        const doAction = () => {
            if (param) return action(rowData[param]);
            return action();
        };
        const showActionButton = () => {
            if (actionButtonHandler && actionButtonParam) {
                return actionButtonHandler(rowData[actionButtonParam]);
            } else {
                return true;
            }
        };

        return tableData(
            <>
                {showActionButton() ? (
                    <Button className="font-mono-2xs line-height-alt-4" type="button" unstyled onClick={doAction}>
                        {displayValue}
                    </Button>
                ) : (
                    <div>{displayValue}</div>
                )}
            </>,
        );
    }

    if (isEditing()) {
        // Make column value editable
        return tableData(
            <input
                aria-label={`${field}-${rowIndex}`}
                className="usa-input"
                onChange={(event) => {
                    setUpdatedRow(event.target.value, field);
                }}
                /* This ensures the value seen in the edit field is
                 * the same as the server-provided data, NOT the
                 * displayed data (in case of transformation/map) */
                defaultValue={displayValue}
            />,
        );
    }

    return columnConfig.valueMap ? tableData(showMappedFieldValue(columnConfig, rowData)) : tableData(displayValue);
};
