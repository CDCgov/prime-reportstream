import { Helmet } from "react-helmet";
import React, { useState } from "react";

import Table, {
    ColumnConfig,
    LegendItem,
    TableConfig,
    DatasetAction,
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

    const [, setValueSet] = useState<ValueSet[]>([]);

    const tableConfig: TableConfig = {
        columns: valueSetColumnConfig,
        rows: valueSetArray,
    };
    /* These items, I'm assuming, are likely to be generated from API response data? */
    const legendItems: LegendItem[] = [
        { label: "Name", value: "HL00005" },
        { label: "Version", value: "2.5.1" },
        { label: "System", value: "HL7" },
        { label: "Reference", value: "Make this linkable" },
    ];
    /* We make this action do what we need it to to add an item */
    const datasetActionItem: DatasetAction = {
        label: "Add item",
        method: async () =>
            setValueSet([
                ...valueSetArray,
                {
                    name: "",
                    system: "",
                    createdAt: "",
                    createdBy: "",
                },
            ]),
    };
    return (
        <Table
            title="ReportStream Value Sets"
            legend={<Legend items={legendItems} />}
            datasetAction={datasetActionItem}
            config={tableConfig}
            enableEditableRows
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
