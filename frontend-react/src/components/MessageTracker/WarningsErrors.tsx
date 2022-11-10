import React from "react";

import { WarningErrorDetail } from "../../config/endpoints/messageTracker";
import Table, { TableConfig } from "../Table/Table";

type WarningsErrorsDisplayProps = {
    title: string;
    data: WarningErrorDetail[];
};

export const WarningsErrors = ({ title, data }: WarningsErrorsDisplayProps) => {
    const tableConfig: TableConfig = {
        columns: [
            {
                dataAttr: "fieldMapping",
                columnHeader: "Field",
            },
            {
                dataAttr: "message",
                columnHeader: "Description",
            },
            {
                dataAttr: "class",
                columnHeader: "Type",
            },
        ],
        rows: data || [],
    };

    return (
        <>
            <section className="margin-bottom-5">
                <Table
                    title={title}
                    classes={"padding-0"}
                    tableRowsClassName={"font-body-xs"}
                    config={tableConfig}
                />
            </section>
        </>
    );
};
