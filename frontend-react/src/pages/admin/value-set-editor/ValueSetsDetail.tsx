import React, {
    useState,
    useEffect,
    Dispatch,
    SetStateAction,
    useMemo,
} from "react";
import { Helmet } from "react-helmet";
import { useParams } from "react-router-dom";

import Table, {
    ColumnConfig,
    DatasetAction,
    TableConfig,
    TableRow,
} from "../../../components/Table/Table";
import {
    useValueSetActivation,
    useValueSetsTable,
    useValueSetUpdate,
} from "../../../hooks/UseValueSets";
import { toHumanReadable } from "../../../utils/misc";
import { ValueSetRow } from "../../../config/endpoints/lookupTables";
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

interface SenderAutomationDataRow extends ValueSetRow {
    id?: number;
}

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

// splices the new row in the list of all rows,
// since we can't save one row at a time
const prepareRowsForSave = (
    row: TableRow | null,
    allRows: SenderAutomationDataRow[],
    valueSetName: string
): ValueSetRow[] => {
    if (row === null) {
        throw new Error("A null row was encountered in saveData");
    }

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

    return strippedArray;
};

const addIdsToRows = (valueSetArray: ValueSetRow[] = []): ValueSetRow[] => {
    return valueSetArray.map((row, index) => {
        return {
            ...row,
            id: index,
        };
    });
};

export const ValueSetsDetailTable = ({
    valueSetName,
    setAlert,
}: {
    valueSetName: string;
    setAlert: Dispatch<SetStateAction<ReportStreamAlert | undefined>>;
}) => {
    const { valueSetArray, error: dataError } =
        useValueSetsTable<ValueSetRow[]>(valueSetName);

    const { saveData } = useValueSetUpdate();
    const { activateTable } = useValueSetActivation();

    useEffect(() => {
        if (dataError) {
            handleErrorWithAlert({
                logMessage: "Error occurred fetching value set",
                error: dataError,
                setAlert,
            });
        }
    }, [dataError, setAlert]);

    const valueSetsWithIds = useMemo(
        () => addIdsToRows(valueSetArray),
        [valueSetArray]
    );

    const tableConfig: TableConfig = useMemo(
        () => ({
            columns: valueSetDetailColumnConfig,
            rows: valueSetsWithIds,
        }),
        [valueSetsWithIds]
    );

    const datasetActionItem: DatasetAction = {
        label: "Add item",
    };

    return (
        <Table
            title="ReportStream Core Values"
            // assume we don't want to allow creating a row if initial fetch failed
            datasetAction={dataError ? undefined : datasetActionItem}
            config={tableConfig}
            enableEditableRows
            editableCallback={async (row) => {
                try {
                    const dataToSave = prepareRowsForSave(
                        row,
                        valueSetsWithIds,
                        valueSetName
                    );
                    const saveResponse = await saveData({
                        data: dataToSave,
                        tableName: valueSetName,
                    });
                    await activateTable({
                        tableVersion: saveResponse.tableVersion,
                        tableName: valueSetName,
                    });
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
