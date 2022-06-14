import { Helmet } from "react-helmet";
import React from "react";

import Table, {
    ColumnConfig,
    LegendItem,
    TableConfig,
} from "../../../components/Table/Table";
import { generateUseLookupTable } from "../../../hooks/UseLookupTable";
import { LookupTables, ValueSet } from "../../../network/api/LookupTableApi";

export const Legend = ({ items }: { items: LegendItem[] }) => {
    const makeItem = (label: string, value: string) => (
        <div key={label} className="display-flex">
            <b>{`${label}:`}</b>
            <span className="padding-left-05">{value}</span>
        </div>
    );
    return (
        <section
            data-testid="table-legend"
            className="display-flex flex-column"
        >
            {items.map((item) => makeItem(item.label, item.value))}
        </section>
    );
};
const valueSetColumnConfig: ColumnConfig[] = [
    {
        dataAttr: "name",
        columnHeader: "Valueset Name",
        feature: {
            link: true,
            linkBasePath: "value-sets/",
        },
    },
    {
        dataAttr: "system",
        columnHeader: "System",
    },
    {
        dataAttr: "createdBy",
        columnHeader: "Created By",
    },
    {
        dataAttr: "createdAt",
        columnHeader: "Created At",
    },
];
const useValueSetsTable = generateUseLookupTable<ValueSet>(
    LookupTables.VALUE_SET
);

const ValueSetsTable = () => {
    const valueSetArray = useValueSetsTable();
    const tableConfig: TableConfig = {
        columns: valueSetColumnConfig,
        rows: valueSetArray,
    };

    return (
        <Table
            title="ReportStream Value Sets"
            config={tableConfig}
            editableCallback={(v: string) => console.log(v)}
        />
    );
};

const ValueSetsIndex = () => {
    return (
        <>
            <Helmet>
                <title>
                    Value Sets | Admin | {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <section className="grid-container">
                <ValueSetsTable />
            </section>
        </>
    );
};

export default ValueSetsIndex;
