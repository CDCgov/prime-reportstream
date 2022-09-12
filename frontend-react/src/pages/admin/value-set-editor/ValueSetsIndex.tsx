import { Helmet } from "react-helmet";
import React, { useEffect, useState } from "react";

import Table, {
    ColumnConfig,
    LegendItem,
    TableConfig,
} from "../../../components/Table/Table";
import {
    useValueSetsMeta,
    useValueSetsTable,
} from "../../../hooks/UseValueSets";
import { StaticAlert } from "../../../components/StaticAlert";
import {
    handleErrorWithAlert,
    ReportStreamAlert,
} from "../../../utils/ErrorUtils";
import {
    LookupTable,
    LookupTables,
    ValueSet,
} from "../../../config/endpoints/lookupTables";

const PAGE_TITLE = process.env.REACT_APP_TITLE; // TODO: move to config

export const Legend = ({ items }: { items: LegendItem[] }) => {
    const makeItem = (label: string, value: string) => (
        <div key={label} className="display-flex">
            <b>{`${label}:`}</b>
            <span className="padding-left-05">{value}</span>
        </div>
    );
    return (
        <section
            data-testid="table-legend"
            className="display-flex flex-column"
        >
            {items.map((item) => makeItem(item.label, item.value))}
        </section>
    );
};
const valueSetColumnConfig: ColumnConfig[] = [
    {
        dataAttr: "name",
        columnHeader: "Valueset Name",
        feature: {
            link: true,
            linkBasePath: "value-sets/",
        },
    },
    {
        dataAttr: "system",
        columnHeader: "System",
    },
    {
        dataAttr: "createdBy",
        columnHeader: "Created By",
    },
    {
        dataAttr: "createdAt",
        columnHeader: "Created At",
    },
];

const toValueSetWithMeta = (
    valueSetArray: ValueSet[] = [],
    valueSetMeta: LookupTable
) => valueSetArray.map((valueSet) => ({ ...valueSet, ...valueSetMeta }));

const ValueSetsTable = () => {
    const [alert, setAlert] = useState<ReportStreamAlert | undefined>();
    const { valueSetMeta, error: metaError } = useValueSetsMeta();
    const { valueSetArray, error: dataError } = useValueSetsTable<ValueSet[]>(
        LookupTables.VALUE_SET
    );

    useEffect(() => {
        if (dataError || metaError) {
            handleErrorWithAlert({
                logMessage: "Error occurred fetching value sets",
                error: dataError || metaError, // this isn't perfect but likely good enough for now
                setAlert,
            });
        }
    }, [metaError, dataError]);

    const tableConfig: TableConfig = {
        columns: valueSetColumnConfig,
        rows: toValueSetWithMeta(valueSetArray, valueSetMeta),
    };

    return (
        <>
            {alert && (
                <StaticAlert
                    type={alert.type}
                    heading={alert.type.toUpperCase()}
                    message={alert.message}
                />
            )}
            <Table title="ReportStream Value Sets" config={tableConfig} />
        </>
    );
};

const ValueSetsIndex = () => {
    return (
        <>
            <Helmet>
                <title>Value Sets | Admin | {PAGE_TITLE}</title>
            </Helmet>
            <section className="grid-container">
                <ValueSetsTable />
            </section>
        </>
    );
};

export default ValueSetsIndex;
