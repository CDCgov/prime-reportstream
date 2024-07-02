import {
    Dispatch,
    ReactNode,
    SetStateAction,
    useCallback,
    useEffect,
    useMemo,
    useState,
} from "react";
import { Helmet } from "react-helmet-async";
import { useParams } from "react-router-dom";

import { withCatchAndSuspense } from "../../../components/RSErrorBoundary/RSErrorBoundary";
import Spinner from "../../../components/Spinner";
import { StaticAlert, StaticAlertType } from "../../../components/StaticAlert";
import Table, {
    ColumnConfig,
    TableConfig,
} from "../../../components/Table/Table";
import { DatasetAction } from "../../../components/Table/TableInfo";
import { TableRowData } from "../../../components/Table/TableRows";
import {
    LookupTable,
    ValueSetRow,
} from "../../../config/endpoints/lookupTables";
import useSessionContext from "../../../contexts/Session/useSessionContext";
import useValueSetActivation from "../../../hooks/api/lookuptables/UseValueSetActivation/UseValueSetActivation";
import useValueSetsMeta from "../../../hooks/api/lookuptables/UseValueSetsMeta/UseValueSetsMeta";
import useValueSetsTable from "../../../hooks/api/lookuptables/UseValueSetsTable/UseValueSetsTable";
import useValueSetUpdate from "../../../hooks/api/lookuptables/UseValueSetsUpdate/UseValueSetUpdate";
import {
    handleErrorWithAlert,
    ReportStreamAlert,
} from "../../../utils/ErrorUtils";
import { toHumanReadable } from "../../../utils/misc";

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
    const { mutateAsync: saveData, isPending: isSaving } = useValueSetUpdate();
    const { mutateAsync: activateTable, isPending: isActivating } =
        useValueSetActivation();
    const { rsConsole } = useSessionContext();
    useEffect(() => {
        if (error) {
            handleErrorWithAlert({
                logMessage: "Error occurred fetching value set",
                error,
                setAlert,
                rsConsole,
            });
        }
    }, [error, rsConsole, setAlert]);

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

    const editCallback = useCallback(
        async (row: any) => {
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
                    rsConsole,
                });
                return;
            }
            setAlert({ type: "success", message: "Value Saved" });
        },
        [
            activateTable,
            rsConsole,
            saveData,
            setAlert,
            valueSetName,
            valueSetsWithIds,
        ],
    );

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
            editableCallback={editCallback}
        />
    );
};

const ValueSetsDetailContent = () => {
    const { valueSetName } = useParams<{ valueSetName: string }>();
    if (!valueSetName) throw new Error("Value set name missing");
    // TODO: when to unset?
    const [alert, setAlert] = useState<ReportStreamAlert | undefined>();

    const { data: valueSetArray } =
        useValueSetsTable<ValueSetRow[]>(valueSetName);
    const { data: valueSetMeta } = useValueSetsMeta(valueSetName);
    const meta = valueSetMeta ?? {
        lookupTableVersionId: 0,
        tableName: "",
        tableVersion: 0,
        isActive: false,
        createdBy: "",
        createdAt: "",
        tableSha256Checksum: "",
    };

    const readableName = useMemo(
        () => toHumanReadable(valueSetName),
        [valueSetName],
    );

    return (
        <>
            <Helmet>
                <title>{`Value Sets | Admin | ${readableName}`}</title>
                <meta
                    property="og:image"
                    content="/assets/img/opengraph/reportstream.png"
                />
                <meta
                    property="og:image:alt"
                    content='"ReportStream" surrounded by an illustration of lines and boxes connected by colorful dots.'
                />
            </Helmet>
            <section className="grid-container">
                <ValueSetsDetailHeader name={readableName} meta={meta} />
                {/* ONLY handles success messaging now */}
                {alert && (
                    <StaticAlert
                        type={alert.type as StaticAlertType}
                        heading={alert.type.toUpperCase()}
                        message={alert.message}
                    />
                )}
                <ValueSetsDetailTable
                    valueSetName={valueSetName}
                    setAlert={setAlert}
                    valueSetData={valueSetArray ?? []}
                />
            </section>
        </>
    );
};
export const ValueSetsDetailPage = () =>
    withCatchAndSuspense(<ValueSetsDetailContent />);

export default ValueSetsDetailPage;
