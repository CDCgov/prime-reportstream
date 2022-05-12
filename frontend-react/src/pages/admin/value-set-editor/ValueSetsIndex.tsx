import { Helmet } from "react-helmet";
import { useState } from "react";

import Table, {
    ColumnConfig,
    DatasetAction,
    Legend,
    LegendItem,
    TableConfig,
} from "../../../components/Table/Table";

/* FAUX DATA AND STUFF TO BE REMOVED WHEN IMPLEMENTING THE API */
interface ValueSet {
    value: string;
    header: string;
    type: string;
}
const sampleValueSetColumnConfig: ColumnConfig[] = [
    {
        dataAttr: "value",
        columnHeader: "Value",
    },
    {
        dataAttr: "header",
        columnHeader: "Column header",
    },
    {
        dataAttr: "type",
        columnHeader: "Value type",
    },
];
/* END OF FAUX DATA AND STUFF TO BE REMOVED WHEN IMPLEMENTING THE API */

const ValueSetsTable = () => {
    /* This would be our API response as reactive state */
    const [sampleValueSetArray, setSampleValueSetArray] = useState<ValueSet[]>([
        {
            value: "Patient ID",
            header: "patient_id",
            type: "Unique identifier",
        },
        {
            value: "Patient last name",
            header: "patient_last_name",
            type: "Text",
        },
    ]);
    /* We'd pass our config and our API response in this */
    const tableConfig: TableConfig = {
        columns: sampleValueSetColumnConfig,
        rows: sampleValueSetArray,
    };
    /* These items, I'm assuming, are likely to be generated from API response data? */
    const legendItems: LegendItem[] = [
        { label: "Name", value: "HL00005" },
        { label: "Version", value: "2.5.1" },
        { label: "System", value: "HL7" },
        { label: "Reference", value: "Make this linkable" },
    ];
    /* We make this action do what we need it to */
    const datasetActionItem: DatasetAction = {
        label: "Add item",
        method: () =>
            setSampleValueSetArray([
                ...sampleValueSetArray,
                {
                    value: "New value",
                    header: "new_value_header",
                    type: "Text",
                },
            ]),
    };
    return (
        <Table
            title="ReportStream Value Sets"
            legend={<Legend items={legendItems} />}
            datasetAction={datasetActionItem}
            config={tableConfig}
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
