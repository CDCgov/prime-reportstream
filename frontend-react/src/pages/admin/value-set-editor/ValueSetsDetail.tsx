import React, { useState, useEffect, Dispatch, SetStateAction } from "react";
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
    LookupTable,
    lookupTableApi,
    ValueSetRow,
} from "../../../network/api/LookupTableApi";
import { StaticAlert } from "../../../components/StaticAlert";
import {
    ReportStreamAlert,
    handleErrorWithAlert,
} from "../../../utils/ErrorUtils";

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
): Promise<LookupTable> => {
    if (row === null) {
        throw new Error("A null row was encountered in saveData");
    }

    const endpointHeaderUpdate =
        lookupTableApi.saveTableData<ValueSetRow[]>(valueSetName);

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

    const updateResult = await axios.post(
        endpointHeaderUpdate.url,
        strippedArray,
        endpointHeaderUpdate
    );

    const endpointHeaderActivate = lookupTableApi.activateTableData(
        updateResult.data.tableVersion,
        valueSetName
    );

    const activateResult = await axios.put(
        endpointHeaderActivate.url,
        valueSetName,
        endpointHeaderActivate
    );
    return activateResult.data;
};

interface SenderAutomationDataRow extends ValueSetRow {
    id?: number;
}

export const ValueSetsDetailTable = ({
    valueSetName,
    setAlert,
}: {
    valueSetName: string;
    setAlert: Dispatch<SetStateAction<ReportStreamAlert | undefined>>;
}) => {
    const [valueSetRows, setValueSetRows] = useState<ValueSetRow[]>(
        [] as ValueSetRow[]
    );
    const [valueSetsVersion, setValueSetVersion] = useState<number>();

    const { valueSetArray, error } = useValueSetsRowTable(
        valueSetName,
        valueSetsVersion
    );

    useEffect(() => {
        if (error) {
            handleErrorWithAlert({
                logMessage: "Error occurred fetching value set",
                error,
                setAlert,
            });
        }
    }, [error, setAlert]);

    useEffect(() => {
        setValueSetRows(valueSetArray);
    }, [valueSetArray]);

    const tableConfig: TableConfig = {
        columns: valueSetDetailColumnConfig,
        rows: valueSetRows,
    };

    const datasetActionItem: DatasetAction = {
        label: "Add item",
    };

    return (
        <Table
            title="ReportStream Core Values"
            // assume we don't want to allow creating a row if initial fetch failed
            datasetAction={error ? undefined : datasetActionItem}
            config={tableConfig}
            enableEditableRows
            editableCallback={async (row) => {
                try {
                    const data = await saveData(
                        row,
                        valueSetRows,
                        valueSetName
                    );
                    setValueSetVersion(data.tableVersion);
                } catch (e: any) {
                    handleErrorWithAlert({
                        logMessage: "Error occurred saving value set",
                        error: e,
                        setAlert,
                    });
                    return;
                }
                setAlert({ type: "success", message: "Value Saved" });
            }}
        />
    );
};

const ValueSetsDetail = () => {
    const { valueSetName } = useParams<{ valueSetName: string }>();
    // TODO: when to unset?
    const [alert, setAlert] = useState<ReportStreamAlert | undefined>();

    return (
        <>
            <Helmet>
                <title>{`Value Sets | Admin | ${valueSetName}`}</title>
            </Helmet>
            <section className="grid-container">
                {/* valueSetsDetailHeader would go here */}
                <h1>{toHumanReadable(valueSetName)}</h1>
                {alert && (
                    <StaticAlert
                        type={alert.type}
                        heading={alert.type.toUpperCase()}
                        message={alert.message}
                    />
                )}
                <ValueSetsDetailTable
                    valueSetName={valueSetName}
                    setAlert={setAlert}
                />
            </section>
        </>
    );
};

export default ValueSetsDetail;
