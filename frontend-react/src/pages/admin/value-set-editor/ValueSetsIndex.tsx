import { Helmet } from "react-helmet-async";

import { withCatchAndSuspense } from "../../../components/RSErrorBoundary/RSErrorBoundary";
import Table, {
    ColumnConfig,
    LegendItem,
    TableConfig,
} from "../../../components/Table/Table";
import {
    LookupTable,
    LookupTables,
    ValueSet,
} from "../../../config/endpoints/lookupTables";
import useValueSetsMeta from "../../../hooks/api/lookuptables/UseValueSetsMeta/UseValueSetsMeta";
import useValueSetsTable from "../../../hooks/api/lookuptables/UseValueSetsTable/UseValueSetsTable";

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
    valueSetMeta: LookupTable = {
        lookupTableVersionId: 0,
        tableName: "",
        tableVersion: 0,
        isActive: false,
        createdBy: "",
        createdAt: "",
        tableSha256Checksum: "",
    },
    valueSetArray: ValueSet[] = [],
) => valueSetArray.map((valueSet) => ({ ...valueSet, ...valueSetMeta }));

const ValueSetsTable = () => {
    const { data: valueSetMeta } = useValueSetsMeta();
    const { data: valueSetArray } = useValueSetsTable<ValueSet[]>(
        LookupTables.VALUE_SET,
    );

    const tableConfig: TableConfig = {
        columns: valueSetColumnConfig,
        rows: toValueSetWithMeta(valueSetMeta, valueSetArray),
    };

    return <Table title="ReportStream Value Sets" config={tableConfig} />;
};
const ValueSetsIndexPage = () => {
    return (
        <>
            <Helmet>
                <title>Value sets - Admin</title>
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
                {withCatchAndSuspense(<ValueSetsTable />)}
            </section>
        </>
    );
};

export default ValueSetsIndexPage;
