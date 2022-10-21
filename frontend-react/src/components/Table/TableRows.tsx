import React, {
    useState,
    useCallback,
    useMemo,
    SetStateAction,
    Dispatch,
} from "react";
import { Button } from "@trussworks/react-uswds";

import { FilterManager } from "../../hooks/filters/UseFilterManager";
import { StringIndexed } from "../../utils/UsefulTypes";

import { RowSideEffect, ColumnConfig } from "./Table";
import { ColumnData } from "./ColumnData";

export type TableRowData = StringIndexed;

interface RowProps {
    columns: ColumnConfig[];
    enableEditableRows?: boolean;
    rowToEdit: number | undefined;
}

interface TableRowsProps extends RowProps {
    rows: TableRowData[];
    filterManager?: FilterManager;
    onSave?: RowSideEffect;
    setRowToEdit: Dispatch<SetStateAction<number | undefined>>;
    className?: string;
}

interface TableRowProps extends RowProps {
    rowData: TableRowData;
    rowIndex: number;
    editButtonLabel?: string;
    updateRow: (value: string, field: string) => void;
    saveRowOrSetEditing: (index?: number) => void;
    isNew: boolean;
}

const createBlankRowForColumns = (columns: ColumnConfig[]) => {
    return columns.reduce((acc, column) => {
        const { dataAttr } = column;
        acc[dataAttr] = null;
        return acc;
    }, {} as TableRowData);
};

const TableRow = ({
    rowIndex,
    columns,
    rowData,
    rowToEdit,
    enableEditableRows,
    updateRow,
    saveRowOrSetEditing,
    editButtonLabel,
    isNew,
}: TableRowProps) => {
    // on a new row, all fields should be editable
    const columnsForDisplay = useMemo(() => {
        if (!isNew) {
            return columns;
        }
        return columns.map((column) => ({
            ...column,
            editable: true,
        }));
    }, [isNew, columns]);

    return (
        <tr key={rowIndex}>
            {columnsForDisplay.map((colConfig, colIndex) => (
                <ColumnData
                    key={`${rowIndex}:${colIndex}:TOP`}
                    rowIndex={rowIndex}
                    colIndex={colIndex}
                    columnConfig={colConfig}
                    rowData={rowData}
                    editing={rowToEdit === rowIndex}
                    setUpdatedRow={updateRow}
                />
            ))}
            {enableEditableRows ? (
                <td key={`${rowIndex}:EDIT`}>
                    <Button
                        type="submit"
                        onClick={() => saveRowOrSetEditing(rowIndex)}
                    >
                        {editButtonLabel}
                    </Button>
                </td>
            ) : null}
        </tr>
    );
};

/* Iterates each row, and then uses the key value from columns.keys()
 * to render each cell in the appropriate column.
 * POSSIBLE TODO: do we want to split out the editing stuff into a plugin / hook / different component?
 */
export const TableRows = ({
    rows,
    onSave = () => Promise.resolve(),
    enableEditableRows,
    filterManager,
    columns,
    setRowToEdit,
    rowToEdit,
    className = "font-mono-2xs", // Default font to mono if not overridden
}: TableRowsProps) => {
    // tracks data changes to row currently being edited
    // TODO: build proper loading state
    const [updatedRow, setUpdatedRow] = useState<TableRowData | null>(null);

    // TODO: typing this input as a string for now
    // but may need to cast it to a number, Date, or parse a JSON string in the future
    // especially if we're dealing with non text type inputs. Otherwise we may end up
    // with difficulty working with new data, or pushing to API without errors
    const updateFieldForRow = (value: string, field: string) => {
        // largely here for typecheck reasons, this represents a case
        // where we're somehow trying to update a row without editing enabled
        if (rowToEdit === undefined) {
            console.error("Editing not enabled or no row to edit");
            return;
        }

        const rowToUpdate = updatedRow ? updatedRow : rowsToDisplay[rowToEdit];
        const rowValues = { ...rowToUpdate };
        // update the field value in the given row
        rowValues[field] = value;
        setUpdatedRow(rowValues);
    };

    // note that this function does not update the data displayed in the table directly
    // it will be the responsibility of the parent component to handle any data updates by
    // updating props passed to the table as a result of the onSave function
    const saveRowOrSetEditing = useCallback(
        (rowIndex?: number) => {
            // if we are in edit mode, and the row being edited matches the
            // row of the button being clicked, then we want to save
            if (rowToEdit !== undefined && rowToEdit === rowIndex) {
                // but if there are no changes to save, just back out of editing
                if (!updatedRow) {
                    setRowToEdit(undefined);
                    return;
                }
                // TODO: implement a loading state here
                return onSave(updatedRow).then(() => {
                    setRowToEdit(undefined);
                    setUpdatedRow(null);
                });
            }
            // otherwise, enable editing mode for this row
            setUpdatedRow(null); // in case we have some weird old irrelevant data in the state
            setRowToEdit(rowIndex);
        },
        [rowToEdit, setRowToEdit, updatedRow, onSave]
    );

    const addingNewRow: boolean = useMemo(
        () => rowToEdit !== undefined && rowToEdit === rows.length,
        [rowToEdit, rows.length]
    );

    // decouple the rows we are displaying from the rows that have been persisted to allow
    // easier editing
    const rowsToDisplay = useMemo(() => {
        if (!addingNewRow) {
            return [...rows];
        }
        // if the row is currently under edit, use that row, otherwise create a blank one
        const newRow = updatedRow || createBlankRowForColumns(columns);
        return [...rows].concat([newRow]);
    }, [rows, addingNewRow, updatedRow, columns]);

    return (
        <tbody className={className}>
            {rowsToDisplay.map((object: TableRowData, rowIndex: number) => {
                // Caps page size when filterManager exists
                if (
                    filterManager &&
                    rowIndex >= filterManager?.pageSettings.size
                ) {
                    return null;
                }
                // note comparison to `rows` length rather than `rowsToDisplay` to make sure
                // this is a row that has been added
                const isNewRow = addingNewRow && rowIndex === rows.length;
                return (
                    <TableRow
                        rowIndex={rowIndex}
                        columns={columns}
                        rowData={object}
                        rowToEdit={rowToEdit}
                        updateRow={updateFieldForRow}
                        enableEditableRows={enableEditableRows}
                        saveRowOrSetEditing={saveRowOrSetEditing}
                        editButtonLabel={
                            rowToEdit === rowIndex ? "Save" : "Edit"
                        }
                        key={rowIndex}
                        isNew={isNewRow}
                    />
                );
            })}
        </tbody>
    );
};
