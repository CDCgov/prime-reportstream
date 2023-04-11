import classnames from "classnames";

import styles from "./Table.module.scss";

export interface TableProps {
    borderless?: boolean;
    children: React.ReactNode;
    compact?: boolean;
    emptyState?: boolean;
    fullWidth?: boolean;
    scrollable?: boolean;
    sortable?: boolean;
    stackedStyle?: "default" | "headers";
    striped?: boolean;
    columnHeaders: string[];
    rowData: string[][];
}

export const Table = ({
    borderless,
    children,
    compact,
    emptyState,
    fullWidth,
    scrollable,
    sortable,
    stackedStyle,
    striped,
    columnHeaders,
    rowData,
}: TableProps) => {
    const classes = classnames("usa-table", {
        "usa-table--borderless": borderless,
        "usa-table--compact": compact,
        "width-full": fullWidth,
        "usa-table--stacked": stackedStyle === "default",
        "usa-table--stacked-header": stackedStyle === "headers",
        "usa-table--striped": striped,
    });

    return (
        <div
            {...(scrollable && {
                className: "usa-table-container--scrollable",
                tabIndex: 0,
            })}
        >
            <table className={classes} data-testid="table">
                <thead>
                    <tr>
                        {columnHeaders.map((header) => {
                            return <td>{header}</td>;
                        })}
                    </tr>
                </thead>
                <tbody>
                    {rowData.map((row) => {
                        return (
                            <tr>
                                {row.map((data) => {
                                    return <td>{data}</td>;
                                })}
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
};
