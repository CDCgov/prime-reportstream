import React from "react";
import { Helmet } from "react-helmet";
import { useParams } from "react-router-dom";
import axios from "axios";

import Table, {
    ColumnConfig,
    DatasetAction,
    TableConfig,
    TableRow,
} from "../../../components/Table/Table";
import { useValueSetsRowTable } from "../../../hooks/UseLookupTable";
import { toHumanReadable } from "../../../utils/misc";
import {
    lookupTableApi,
    LookupTables,
    ValueSetRow,
} from "../../../network/api/LookupTableApi";
import { showError } from "../../../components/AlertNotifications";

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
/* 

  all of this is to support a legend on the page that has been removed from MVP

  this can be added back once we have resources available to make this dynamic

  const legendItems: LegendItem[] = [
      { label: "Name", value: valueSetName },
      { label: "Version", value: "2.5.1" },
      { label: "System", value: "HL7" },
      {
          label: "Reference",
          value: "HL7 guidance for ethnicity (Make this linkable)",
      },
  ];
  
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

*/

// const defaultValueSetRows = [
//     {
//         name: "ethnicity",
//         display: "American Indian or Alaska Native",
//         code: "1002-5",
//         version: "2.5.1",
//         system: "HL7",
//     },
//     {
//         name: "ethnicity",
//         display: "Asian",
//         code: "2028-9",
//         version: "2.5.4",
//         system: "Name of org",
//     },
//     {
//         name: "ethnicity",
//         display: "Black or African American",
//         code: "2054-5",
//         version: "2.3.0",
//         system: "HL7",
//     },
// ];

const saveData = async (row: TableRow | null) => {
    debugger;
    const x: ValueSetRow[] = [
        {
            name: "ethnicity",
            display: row?.display,
            code: row?.code,
            version: row?.version,
        },
    ];

    const endpointHeaderUpdate = lookupTableApi.saveTableData<ValueSetRow>(
        LookupTables.VALUE_SET_ROW
    );
    try {
        return await axios
            .post(endpointHeaderUpdate.url, x)
            // .get<LookupTables.VALUE_SET_ROW>(endpointHeader.url, endpointHeader)
            .then((response) => response.data);
    } catch (e: any) {
        console.trace(e);
        showError(e.toString());
        return [];
    }

    // const endpointHeaderActivate = lookupTableApi.activateTableData(
    //     LookupTables.VALUE_SET_ROW
    // );
    // try {
    //     return await axios
    //         .post(endpointHeader.url, x)
    //         // .get<LookupTables.VALUE_SET_ROW>(endpointHeader.url, endpointHeader)
    //         .then((response) => response.data);
    // } catch (e: any) {
    //     console.trace(e);
    //     showError(e.toString());
    //     return [];
    // }
};

const ValueSetsDetailTable = ({ valueSetName }: { valueSetName: string }) => {
    debugger;
    const valueSetRowArray = useValueSetsRowTable(valueSetName);

    const tableConfig: TableConfig = {
        columns: valueSetDetailColumnConfig,
        rows: valueSetRowArray,
    };
    /* We make this action do what we need it to to add an item */
    const datasetActionItem: DatasetAction = {
        label: "Add item",
    };
    return (
        <Table
            title="ReportStream Core Values"
            datasetAction={datasetActionItem}
            config={tableConfig}
            enableEditableRows
            editableCallback={async (row) => {
                return await saveData(row);
            }}
            // editableCallback={(row) => {
            //     console.log("!!! saving row", row);
            //     return Promise.resolve();
            // }}
        />
    );
};

const ValueSetsDetail = () => {
    const { valueSetName } = useParams<{ valueSetName: string }>();
    // TODO: fetch the value set from the API

    return (
        <>
            <Helmet>
                <title>{`Value Sets | Admin | ${valueSetName}`}</title>
            </Helmet>
            <section className="grid-container">
                {/* valueSetsDetailHeader would go here */}
                <h1>{toHumanReadable(valueSetName)}</h1>
                <ValueSetsDetailTable valueSetName={valueSetName} />
            </section>
        </>
    );
};

export default ValueSetsDetail;
