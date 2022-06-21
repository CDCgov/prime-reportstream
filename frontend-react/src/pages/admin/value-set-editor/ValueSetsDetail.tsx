import React from "react";
import { Helmet } from "react-helmet";
import { useParams } from "react-router-dom";

import Table, {
    ColumnConfig,
    DatasetAction,
    LegendItem,
    TableConfig,
} from "../../../components/Table/Table";
import { useValueSetsRowTable } from "../../../hooks/UseLookupTable";

import { Legend } from "./ValueSetsIndex";

const valueSetDetailColumnConfig: ColumnConfig[] = [
    {
        dataAttr: "display",
        columnHeader: "Display",
        editable: true,
    },
    {
        dataAttr: "code",
        columnHeader: "Code",
        editable: true,
    },
    {
        dataAttr: "version",
        columnHeader: "Version",
        editable: true,
    },
    {
        dataAttr: "system",
        columnHeader: "System",
        editable: true,
    },
];

const ValueSetsDetailTable = ({ valueSetName }: { valueSetName: string }) => {
    const valueSetRowArray = useValueSetsRowTable(valueSetName);
    // const [sampleValueSetRows, setSampleValueSetRows] = useState<ValueSet[]>(defaultValueSetRows);

    const tableConfig: TableConfig = {
        columns: valueSetDetailColumnConfig,
        rows: valueSetRowArray,
    };
    /* These items, I'm assuming, are likely to be generated from API response data? */
    const legendItems: LegendItem[] = [
        { label: "Name", value: valueSetName },
        { label: "Version", value: "2.5.1" },
        { label: "System", value: "HL7" },
        {
            label: "Reference",
            value: "HL7 guidance for ethnicity (Make this linkable)",
        },
    ];
    /* We make this action do what we need it to to add an item */
    const datasetActionItem: DatasetAction = {
        label: "Add item",
    };
    return (
        <Table
            title="ReportStream Core Values"
            legend={<Legend items={legendItems} />}
            datasetAction={datasetActionItem}
            config={tableConfig}
            enableEditableRows
            editableCallback={(row) => {
                console.log("!!! saving row", row);
                return Promise.resolve();
            }}
        />
    );
};

// currently a placeholder based on design doc
// TODO: does this need to be more dynamic than we've made it here?
const ValueSetsDetailHeader = ({
    valueSetName,
    updatedAt,
    updatedBy,
}: {
    valueSetName: string;
    updatedAt: Date;
    updatedBy: string;
}) => {
    return (
        <>
            <h1>{valueSetName}</h1>
            <p>
                File will fail if numeric values or test values are not entered
                using accepted values or field is left blank.
            </p>
            <p>
                Accepted values come from values mapped to LOINC codes you can
                find in the PHN VADS system (needs link).
            </p>
            <p>
                <b>Last update:</b> {updatedAt.toString()}
            </p>
            <p>
                <b>Updated by:</b> {updatedBy}
            </p>
        </>
    );
};

const ValueSetsDetail = () => {
    const { valueSetName } = useParams<{ valueSetName: string }>();
    // TODO: fetch the value set from the API

    const placeholderUpdatedAt = new Date();
    const placeholderUpdatedBy = "fake_email_address@cdc.gov";

    return (
        <>
            <Helmet>
                <title>{`Value Sets | Admin | ${valueSetName}`}</title>
            </Helmet>
            <section className="grid-container">
                <ValueSetsDetailHeader
                    valueSetName={valueSetName}
                    updatedAt={placeholderUpdatedAt}
                    updatedBy={placeholderUpdatedBy}
                />
                <ValueSetsDetailTable valueSetName={valueSetName} />
            </section>
        </>
    );
};

export default ValueSetsDetail;
