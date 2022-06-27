import React, { useEffect, useMemo, useState } from "react";
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

const saveData = async (
    row: TableRow | null,
    allRows: SenderAutomationDataRow[],
    valueSetName: string
) => {
    if (row === null) {
        showError("A null row was encountered in saveData()");
        return;
    }

    const endpointHeaderUpdate = lookupTableApi.saveTableData<ValueSetRow[]>(
        LookupTables.VALUE_SET_ROW
    );

    const index = allRows.findIndex((r) => r.id === row.id);
    allRows.splice(index, 1, {
        name: valueSetName,
        display: row.display,
        code: row.code,
        version: row.version,
    });

    // must strip all the "id" fields from the JSON before posting to the API, otherwise it 400s
    const strippedArray = allRows.map(
        (set: {
            name: string;
            display: string;
            code: string;
            version: string;
        }) => ({
            name: set.name,
            display: set.display,
            code: set.code,
            version: set.version,
        })
    );

    try {
        let updateResult = await axios
            .post(endpointHeaderUpdate.url, strippedArray, endpointHeaderUpdate)
            .then((response) => response.data);

        const endpointHeaderActivate = lookupTableApi.activateTableData(
            updateResult.tableVersion,
            LookupTables.VALUE_SET_ROW
        );

        return await axios
            .put(
                endpointHeaderActivate.url,
                LookupTables.VALUE_SET_ROW,
                endpointHeaderActivate
            )
            .then((response) => response.data);
    } catch (e: any) {
        console.trace(e);
        showError(e.toString());
        return [];
    }
};

interface SenderAutomationDataRow extends ValueSetRow {
    id?: number;
}

const prepareRows = (
    valueSetRowArray: ValueSetRow[],
    valueSetName: string
): { rowsForDisplay: any[]; allRows: any[] } => {
    return valueSetRowArray.reduce(
        (acc, row, index) => {
            let mapped: SenderAutomationDataRow = {
                name: row.name,
                display: row.display,
                code: row.code,
                version: row.version,
            };
            if (row.name === valueSetName) {
                mapped.id = index;
                acc.rowsForDisplay.push(mapped);
                acc.allRows.push(mapped);
                return acc;
            }
            acc.allRows.push(mapped);
            return acc;
        },
        { rowsForDisplay: [], allRows: [] } as {
            rowsForDisplay: any[];
            allRows: any[];
        }
    );
};

const ValueSetsDetailTable = ({ valueSetName }: { valueSetName: string }) => {
    const [valueSetRows, setValueSetRows] = useState<ValueSetRow[]>(
        [] as ValueSetRow[]
    );
    const [valueSetsVersion, setValueSetVersion] = useState<number>();

    const valueSetRowArray = useValueSetsRowTable(
        valueSetName,
        valueSetsVersion
    );

    useEffect(() => {
        setValueSetRows(valueSetRowArray);
    }, [valueSetRowArray]);

    const { allRows, rowsForDisplay } = useMemo(() => {
        return prepareRows(valueSetRows, valueSetName);
    }, [valueSetRows, valueSetName]);

    const tableConfig: TableConfig = {
        columns: valueSetDetailColumnConfig,
        rows: rowsForDisplay,
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
                const data = await saveData(row, allRows, valueSetName);
                console.log("!!! saved data", data);
                setValueSetVersion(data.tableVersion);
            }}
        />
    );
};

const ValueSetsDetail = () => {
    const { valueSetName } = useParams<{ valueSetName: string }>();

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
