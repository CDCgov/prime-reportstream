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
    useValueSetsMeta,
    useValueSetsTable,
    useValueSetUpdate,
} from "../../../hooks/UseValueSets";
import { toHumanReadable } from "../../../utils/misc";
import {
    LookupTable,
    ValueSetRow,
} from "../../../config/endpoints/lookupTables";
import { StaticAlert } from "../../../components/StaticAlert";
import {
    ReportStreamAlert,
    handleErrorWithAlert,
} from "../../../utils/ErrorUtils";
import { ReactNode } from "react-markdown/lib/react-markdown";

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

// This is displaying all of what seems to be the relevant data from the meta call
// As well as hardcoded info from the designs. This needs a review
const ValueSetsLegend = ({ meta }: { meta: LookupTable }) => (
    <>
        <div>
            <b>Name: </b>
            {meta.tableName}
        </div>
        <div>
            <b>Table Version: </b>
            {meta.tableVersion}
        </div>
        <div>
            <b>Version ID: </b>
            {meta.lookupTableVersionId}
        </div>
        <div>
            <b>Version: </b>2.5.1
        </div>
        <div>
            <b>System: </b>HL7
        </div>
        <div>
            <b>Reference: </b>
            <a>HL7 Guidance for {meta.tableName}</a>
        </div>
    </>
);

// currently a placeholder based on design doc
// This needs a review, especially since we don't have update meta, only creation
const ValueSetsDetailHeader = ({
    name,
    meta,
}: {
    name: string;
    meta: LookupTable;
}) => {
    const { createdAt, createdBy } = meta;
    return (
        <>
            <h1>{name}</h1>
            <p>
                File will fail if numeric values or test values are not entered
                using accepted values or field is left blank.
            </p>
            <p>
                Accepted values come from values mapped to LOINC codes you can
                find in the PHN VADS system (needs link).
            </p>
            <p>
                <b>Last update:</b> {createdAt.toString()}
            </p>
            <p>
                <b>Updated by:</b> {createdBy}
            </p>
        </>
    );
};

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
    error,
    valueSetData,
    Legend,
}: {
    valueSetName: string;
    setAlert: Dispatch<SetStateAction<ReportStreamAlert | undefined>>;
    valueSetData: ValueSetRow[];
    error?: Error;
    Legend?: ReactNode;
}) => {
    const { saveData } = useValueSetUpdate();
    const { activateTable } = useValueSetActivation();

    useEffect(() => {
        if (error) {
            handleErrorWithAlert({
                logMessage: "Error occurred fetching value set",
                error,
                setAlert,
            });
        }
    }, [error, setAlert]);

    const valueSetsWithIds = useMemo(
        () => addIdsToRows(valueSetData),
        [valueSetData]
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
            classes={"rs-no-padding"}
            legend={Legend}
            // assume we don't want to allow creating a row if initial fetch failed
            datasetAction={error ? undefined : datasetActionItem}
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

    const { valueSetArray, error } =
        useValueSetsTable<ValueSetRow[]>(valueSetName);
    const { valueSetMeta, error: metaError } = useValueSetsMeta();

    const readableName = useMemo(
        () => toHumanReadable(valueSetName),
        [valueSetName]
    );

    useEffect(() => {
        if (metaError) {
            handleErrorWithAlert({
                logMessage: "Error occurred fetching value set meta",
                error: metaError,
                setAlert,
            });
        }
    }, [metaError, setAlert]);

    return (
        <>
            <Helmet>
                <title>{`Value Sets | Admin | ${readableName}`}</title>
            </Helmet>
            <section className="grid-container">
                <ValueSetsDetailHeader
                    name={readableName}
                    meta={valueSetMeta}
                />
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
                    valueSetData={valueSetArray || []}
                    error={error}
                    Legend={<ValueSetsLegend meta={valueSetMeta} />}
                />
            </section>
        </>
    );
};

export default ValueSetsDetail;
