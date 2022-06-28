import { Helmet } from "react-helmet";
import React, { useEffect, useState } from "react";

import Table, {
    ColumnConfig,
    LegendItem,
    TableConfig,
} from "../../../components/Table/Table";
import { useValueSetsTable } from "../../../hooks/UseLookupTable";
import { StaticAlert } from "../../../components/StaticAlert";
import {
    handleErrorWithAlert,
    ReportStreamAlert,
} from "../../../utils/ErrorUtils";

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

const ValueSetsTable = () => {
    const [alert, setAlert] = useState<ReportStreamAlert | undefined>();
    const { valueSetArray, error } = useValueSetsTable();

    useEffect(() => {
        if (error) {
            handleErrorWithAlert({
                logMessage: "Error occurred fetching value sets",
                error,
                setAlert,
            });
        }
    }, [error]);

    const tableConfig: TableConfig = {
        columns: valueSetColumnConfig,
        rows: valueSetArray,
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
                <title>
                    Value Sets | Admin | {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <section className="grid-container">
                <ValueSetsTable />
            </section>
        </>
    );
};

export default ValueSetsIndex;
