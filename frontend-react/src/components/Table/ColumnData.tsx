import React, { ReactNode } from "react";
import { NavLink } from "react-router-dom";

import { ColumnConfig, LinkableColumn, ActionableColumn } from "./Table";
import { TableRowData } from "./TableRows";

export interface ColumnProps {
    rowIndex: number;
    colIndex: number;
    rowData: TableRowData;
    columnConfig: ColumnConfig;
    editing?: boolean;
    setUpdatedRow?: (value: any, field: string) => void;
}

const showMappedFieldValue = (
    columnConfig: ColumnConfig,
    rowData: TableRowData
) => {
    const rawFieldValue = rowData[columnConfig.dataAttr];
    if (columnConfig.valueMap) {
        return (
            <span>
                {columnConfig.valueMap?.get(rawFieldValue) || rawFieldValue}
            </span>
        );
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
    setUpdatedRow = () => {},
}: ColumnProps) => {
    // Util functions
    // TODO: move these functions outside of the render

    // Easy-to-read way to transform value
    const transform = (
        transformFunc: Function,
        transformVal: string | number
    ) => {
        return transformFunc(transformVal);
    };
    // Runtime type checking for ColumnFeature
    const hasFeature = (attr: string): boolean => {
        if (!columnConfig.feature) return false;
        return Object.keys(columnConfig.feature).includes(attr);
    };
    // Editing state indicator
    const isEditing = (): boolean =>
        (editing && columnConfig.editable) || false;

    // <td> wrapper w/ key
    const tableData = (child: ReactNode) => (
        <td key={`${rowIndex}:${colIndex}`}>{child}</td>
    );

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
            <NavLink
                className="usa-link"
                to={`${feature.linkBasePath || ""}${
                    rowData[feature.linkAttr || field]
                }`}
            >
                {columnConfig.valueMap
                    ? showMappedFieldValue(columnConfig, rowData)
                    : displayValue}
            </NavLink>
        );
    }

    if (hasFeature("action")) {
        // Make column value actionable
        const { action, param } = columnConfig.feature as ActionableColumn;

        if (!rowData[param!!]) {
            console.warn(`The row attribute '${param}' could not be found`);
        }

        const doAction = () => {
            if (param) return action(rowData[param]);
            return action();
        };
        return tableData(
            <button
                className="usa-link bg-transparent border-transparent"
                onClick={() => doAction()}
            >
                {displayValue}
            </button>
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
            />
        );
    }

    return columnConfig.valueMap
        ? tableData(showMappedFieldValue(columnConfig, rowData))
        : tableData(displayValue);
};
