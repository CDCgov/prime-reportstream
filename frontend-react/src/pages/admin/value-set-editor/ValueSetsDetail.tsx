import React, {
    useState,
    Dispatch,
    SetStateAction,
    useMemo,
    useEffect,
} from "react";
import { Helmet } from "react-helmet-async";
import { useParams } from "react-router-dom";
import { ReactNode } from "react-markdown/lib/react-markdown";

import Table, {
    ColumnConfig,
    TableConfig,
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
import { StaticAlert, StaticAlertType } from "../../../components/StaticAlert";
import {
    handleErrorWithAlert,
    ReportStreamAlert,
} from "../../../utils/ErrorUtils";
import { MemberType } from "../../../hooks/UseOktaMemberships";
import { AuthElement } from "../../../components/AuthElement";
import { withCatchAndSuspense } from "../../../components/RSErrorBoundary";
import Spinner from "../../../components/Spinner";
import { TableRowData } from "../../../components/Table/TableRows";
import { DatasetAction } from "../../../components/Table/TableInfo";

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
                <b>Last update:</b> {createdAt}
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
    row: TableRowData | null,
    allRows: SenderAutomationDataRow[],
    valueSetName: string,
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
        }),
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
    valueSetData,
    error,
    Legend,
}: {
    valueSetName: string;
    setAlert: Dispatch<SetStateAction<ReportStreamAlert | undefined>>;
    valueSetData: ValueSetRow[];
    error?: Error;
    Legend?: ReactNode; //  not using this yet, but may want to some day
}) => {
    const { saveData, isSaving } = useValueSetUpdate();
    const { activateTable, isActivating } = useValueSetActivation();
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
        [valueSetData],
    );

    const tableConfig: TableConfig = useMemo(
        () => ({
            columns: valueSetDetailColumnConfig,
            rows: valueSetsWithIds,
        }),
        [valueSetsWithIds],
    );

    const datasetActionItem: DatasetAction = {
        label: "Add item",
    };
    /* Mutations do not support Suspense */
    if (isSaving || isActivating) return <Spinner />;
    return (
        <Table
            title="ReportStream Core Values"
            classes={"rs-no-padding"}
            legend={Legend}
            // assume we don't want to allow creating a row if initial fetch failed
            datasetAction={datasetActionItem}
            config={tableConfig}
            enableEditableRows
            editableCallback={async (row) => {
                try {
                    const dataToSave = prepareRowsForSave(
                        row,
                        valueSetsWithIds,
                        valueSetName,
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

const ValueSetsDetailContent = () => {
    const { valueSetName } = useParams<{ valueSetName: string }>();
    // TODO: when to unset?
    const [alert, setAlert] = useState<ReportStreamAlert | undefined>();

    const { valueSetArray } = useValueSetsTable<ValueSetRow[]>(valueSetName!!);
    const { valueSetMeta } = useValueSetsMeta(valueSetName);

    const readableName = useMemo(
        () => toHumanReadable(valueSetName!!),
        [valueSetName],
    );

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
                {/* ONLY handles success messaging now */}
                {alert && (
                    <StaticAlert
                        type={alert.type as StaticAlertType}
                        heading={alert.type.toUpperCase()}
                        message={alert.message}
                    />
                )}
                <ValueSetsDetailTable
                    valueSetName={valueSetName!!}
                    setAlert={setAlert}
                    valueSetData={valueSetArray || []}
                />
            </section>
        </>
    );
};
export const ValueSetsDetail = () =>
    withCatchAndSuspense(<ValueSetsDetailContent />);
export const ValueSetsDetailWithAuth = () => (
    <AuthElement
        element={<ValueSetsDetail />}
        requiredUserType={MemberType.PRIME_ADMIN}
    />
);
