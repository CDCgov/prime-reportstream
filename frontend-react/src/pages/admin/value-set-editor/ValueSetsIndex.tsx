import { Helmet } from "react-helmet";

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
const sampleValueSetArray: ValueSet[] = [
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
];
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
    const tableConfig: TableConfig = {
        columns: sampleValueSetColumnConfig,
        rows: sampleValueSetArray,
    };
    const legendItems: LegendItem[] = [{ label: "Name", value: "HL00005" }];
    const datasetActionItem: DatasetAction = {
        label: "Console log",
        method: () => console.log("Test"),
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
