import React, { useState, useCallback } from "react";
import { Button } from "@trussworks/react-uswds";

import { FilterManager } from "../../hooks/filters/UseFilterManager";

import { RowSideEffect, TableRow, ColumnConfig } from "./Table";
import { ColumnData } from "./ColumnData";

interface TableRowProps {
    rows: TableRow[];
    columns: ColumnConfig[];
    filterManager?: FilterManager;
    enableEditableRows?: boolean;
    onSave?: RowSideEffect;
}

/* Iterates each row, and then uses the key value from columns.keys()
 * to render each cell in the appropriate column. */
export const TableRows = ({
    rows,
    onSave = () => Promise.resolve(),
    enableEditableRows,
    filterManager,
    columns,
}: TableRowProps) => {
    // tracks which row is currently being edited
    const [editing, setEditing] = useState<number | undefined>();
    // tracks data changes to row currently being edited
    // TODO: build proper loading state
    const [updatedRow, setUpdatedRow] = useState<TableRow | null>(null);

    const editableRowButtonValue = (isEditing: boolean) =>
        isEditing ? "Save" : "Edit";

    // TODO: typing this input as a string for now
    // but may need to cast it to a number, Date, or parse a JSON string in the future
    // especially if we're dealing with non text type inputs. Otherwise we may end up
    // with difficulty working with new data, or pushing to API without errors
    const updateRow = (value: string, field: string) => {
        // largely here for typecheck reasons, this represents a case
        // where we're somehow trying to update a row without editing enabled
        if (editing === undefined) {
            console.error("Editing not enabled?");
            return;
        }
        const rowToUpdate = rows[editing];
        const rowValues = { ...rowToUpdate };
        // update the field value in the given row
        rowValues[field] = value;
        setUpdatedRow(rowValues);
    };

    // note that this function does not update the data displayed in the table directly
    // it will be the responsibility of the parent component to handle any data updates by
    // updating props passed to the table as a result of the onSave function
    const saveRowOrSetEditing = useCallback(
        (rowIndex) => {
            // if we are in edit mode, and the row being edited matches the
            // row of the button being clicked, then we want to save
            if (editing !== undefined && editing === rowIndex) {
                // but if there are no changes to save, just back out of editing
                if (!updatedRow) {
                    console.log("No changes to save");
                    setEditing(undefined);
                    return;
                }
                // TODO: implement a loading state here
                return onSave(updatedRow).then(() => {
                    setEditing(undefined);
                    setUpdatedRow(null);
                });
            }
            // otherwise, enable editing mode for this row
            setUpdatedRow(null); // in case we have some weird old irrelevant data in the state
            setEditing(rowIndex);
        },
        [editing, updatedRow, onSave]
    );

    return (
        <>
            {rows.map((object: TableRow, rowIndex: number) => {
                // Caps page size when filterManager exists
                if (
                    filterManager &&
                    rowIndex >= filterManager?.pageSettings.size
                )
                    return null;
                return (
                    <tr key={rowIndex}>
                        {columns.map((colConfig, colIndex) => (
                            <ColumnData
                                key={`${rowIndex}:${colIndex}:TOP`}
                                rowIndex={rowIndex}
                                colIndex={colIndex}
                                columnConfig={colConfig}
                                rowData={object}
                                editing={editing === rowIndex}
                                setUpdatedRow={updateRow}
                            />
                        ))}
                        {enableEditableRows ? (
                            <td key={`${rowIndex}:EDIT`}>
                                <Button
                                    type="submit"
                                    onClick={() =>
                                        saveRowOrSetEditing(rowIndex)
                                    }
                                >
                                    {editableRowButtonValue(
                                        editing === rowIndex
                                    )}
                                </Button>
                            </td>
                        ) : null}
                    </tr>
                );
            })}
        </>
    );
};
