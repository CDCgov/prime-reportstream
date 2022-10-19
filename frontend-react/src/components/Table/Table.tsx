/* Makes row objects string-indexed */
import { Button } from "@trussworks/react-uswds";
import React, { ReactNode, useMemo, useCallback, useState } from "react";

import Pagination, { PaginationProps } from "../../components/Table/Pagination";
import { FilterManager } from "../../hooks/filters/UseFilterManager";

import { TableRows } from "./TableRows";
import { TableHeaders } from "./TableHeaders";

export interface ActionableColumn {
    action: Function;
    param?: string;
}

export interface LinkableColumn {
    link: boolean;
    linkBasePath?: string;
    linkAttr?: string; // if no linkAttr is given, defaults to dataAttr
}

// each table row will be a map keyed off the dataAttr value of
// a column from the column config from the same tableConfig.
// values will largely be assumed to be primitive values, or values
// to be passed into a transform function defined in the column config
export interface TableRow {
    [key: string]: any;
}

/** @alias for any type of feature column */
type ColumnFeature = ActionableColumn | LinkableColumn;
/** ColumnConfig tells the Table element how to render each column
 *
 * @property dataAttr - Name of the object attribute to be rendered in the column
 * @property columnHeader - The column name
 * @property sortable - boolean
 * @property link - boolean indicating column values are links
 * @property linkAttr - the attribute to plug into the link url
 * @property valueMap - provides key/value pairs to map API values to UI values
 * @property transform - a function used to transform values within the column */
export interface ColumnConfig {
    dataAttr: string;
    columnHeader: string;
    feature?: ColumnFeature;
    sortable?: boolean;
    valueMap?: Map<string | number, any>;
    transform?: Function;
    editable?: boolean;
    localSort?: boolean;
}

export interface TableConfig {
    columns: Array<ColumnConfig>;
    rows: Array<TableRow>;
}

export interface DatasetAction {
    label: string;
    method?: Function;
}

export interface DatasetActionProps extends DatasetAction {
    disabled: boolean;
}

export type RowSideEffect = (row: TableRow | null) => Promise<void>;

/** Configuration pattern for Table component
 * @remarks Working to deprecate cursorManager for paginationProps */
export interface TableProps {
    config: TableConfig;
    title?: string;
    /* The Legend component is the responsibility
     * of the parent to pass in, allowing it to be as
     * versatile as possible */
    legend?: ReactNode;
    datasetAction?: DatasetAction;
    filterManager?: FilterManager;
    paginationProps?: PaginationProps;
    enableEditableRows?: boolean;
    editableCallback?: RowSideEffect;
    classes?: string;
}

export interface LegendItem {
    label: string;
    value: string;
}

/** A configurable USWDS table for easy reuse across ReportStream
 * @remarks In the process of deprecating cursorManager and replacing with paginationProps
 * which gives us number pagination
 * */
const Table = ({
    config,
    title,
    legend,
    datasetAction,
    filterManager,
    paginationProps,
    enableEditableRows,
    editableCallback = () => Promise.resolve(),
    classes,
}: TableProps) => {
    const [rowToEdit, setRowToEdit] = useState<number | undefined>();

    /* memoizedRows is the source of truth for rendered table rows. If a local
     * sort is applied, this function reactively sorts the rows passed in. If
     * the sort column, order, or localSort value change in SortSettings,
     * this reactively updates to account for that, too. */
    const memoizedRows = useMemo(() => {
        if (!config?.rows.length) {
            return [];
        }
        const column = filterManager?.sortSettings?.column || "";
        const locally = filterManager?.sortSettings?.locally || false;
        const localOrder = filterManager?.sortSettings?.localOrder || "DESC";
        const valueType = typeof config?.rows[0]?.[column];
        if (locally) {
            switch (valueType) {
                case "string": {
                    return config.rows.sort((a, b) =>
                        localOrder === "ASC"
                            ? a[column].localeCompare(b[column])
                            : b[column].localeCompare(a[column])
                    );
                }
                case "bigint":
                case "number": {
                    return config.rows.sort((a, b) =>
                        localOrder === "ASC"
                            ? a[column] - b[column]
                            : b[column] - a[column]
                    );
                }
            }
        }
        return config.rows;
    }, [config.rows, filterManager?.sortSettings]);

    const wrapperClasses = useMemo(
        () => `grid-container margin-bottom-10 ${classes}`,
        [classes]
    );

    const addRow = useCallback(() => {
        setRowToEdit(memoizedRows.length);
    }, [memoizedRows, setRowToEdit]);

    // this button will be placed above the rendered table and on `click` will run an arbitrary function
    // passed in from the Table's parent, or an addRow function defined by the Table.
    // in order to avoid problems around timing, takes a `disabled` prop.
    // TODO: split this out of Table component
    const DatasetActionButton = ({
        label,
        method = () => {},
        disabled,
    }: DatasetActionProps) => (
        <Button type={"button"} onClick={() => method()} disabled={disabled}>
            {label}
        </Button>
    );

    const TableInfo = () => {
        return (
            <div className="grid-col-12 display-flex flex-align-end flex-justify-between">
                <div className="grid-col-8 display-flex flex-column">
                    {title ? <h2>{title}</h2> : null}
                    {legend ? legend : null}
                </div>
                <div className="grid-col-2 display-flex flex-column">
                    {datasetAction ? (
                        <DatasetActionButton
                            label={datasetAction.label}
                            method={datasetAction.method || addRow}
                            disabled={!!rowToEdit}
                        />
                    ) : null}
                </div>
            </div>
        );
    };

    return (
        <div className={wrapperClasses}>
            <TableInfo />
            <div className="grid-col-12">
                <table
                    className="usa-table usa-table--borderless usa-table--striped prime-table"
                    aria-label="Submission history from the last 30 days"
                >
                    <TableHeaders
                        config={config}
                        filterManager={filterManager}
                        enableEditableRows={enableEditableRows}
                    />
                    <TableRows
                        rows={memoizedRows}
                        onSave={editableCallback}
                        enableEditableRows={enableEditableRows}
                        filterManager={filterManager}
                        columns={config.columns}
                        rowToEdit={rowToEdit}
                        setRowToEdit={setRowToEdit}
                    />
                </table>
                {paginationProps && <Pagination {...paginationProps} />}
            </div>
        </div>
    );
};

export default Table;
