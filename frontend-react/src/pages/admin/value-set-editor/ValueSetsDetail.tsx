import React from "react";
import { Helmet } from "react-helmet";
import { useParams } from "react-router-dom";

import Table, {
    ColumnConfig,
    DatasetAction,
    LegendItem,
    TableConfig,
} from "../../../components/Table/Table";

import { Legend } from "./ValueSetsIndex";

const valueSetColumns: ColumnConfig[] = [
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

const defaultValueSetRows = [
    {
        display: "American Indian or Alaska Native",
        code: "1002-5",
        version: "2.5.1",
        system: "HL7",
    },
    {
        display: "Asian",
        code: "2028-9",
        version: "2.5.4",
        system: "Name of org",
    },
    {
        display: "Black or African American",
        code: "2054-5",
        version: "2.3.0",
        system: "HL7",
    },
];

/* END OF FAUX DATA AND STUFF TO BE REMOVED WHEN IMPLEMENTING THE API */

const ValueSetsDetailTable = ({ valueSetName }: { valueSetName: string }) => {
    /* This would be replaced by our API response as reactive state (useResource) */
    // const [sampleValueSetRows, setSampleValueSetRows] = useState<ValueSet[]>(defaultValueSetRows);

    /* We'd pass our config and our API response in this */
    const tableConfig: TableConfig = {
        columns: valueSetColumns,
        rows: defaultValueSetRows,
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
        method: () => {
            console.log("!!!! will add a new value. Functionality to come...");
        },
    };
    return (
        <Table
            title="ReportStream Value Sets"
            legend={<Legend items={legendItems} />}
            datasetAction={datasetActionItem}
            config={tableConfig}
            enableEditableRows
            editableCallback={() =>
                console.log(
                    "!!! this is not implemented in table yet, so you will not see this log"
                )
            }
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
                File will fail if numberic values or test values are not entered
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
                <title>Value Sets | Admin | {valueSetName}</title>
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
