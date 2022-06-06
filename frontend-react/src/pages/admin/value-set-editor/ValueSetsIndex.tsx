import { Helmet } from "react-helmet";
import React, { useEffect, useState } from "react";
import { useOktaAuth } from "@okta/okta-react";

import Table, {
    ColumnConfig,
    LegendItem,
    TableConfig,
    DatasetAction,
} from "../../../components/Table/Table";
import {
    getStoredOrg,
    getStoredSenderName,
} from "../../../contexts/SessionStorageTools";

/* FAUX DATA AND STUFF TO BE REMOVED WHEN IMPLEMENTING THE API */
interface ValueSet {
    name: string;
    createdBy: string;
    createdAt: string;
    system: string;
}

const Legend = ({ items }: { items: LegendItem[] }) => {
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
const sampleValueSetColumnConfig: ColumnConfig[] = [
    {
        dataAttr: "name",
        columnHeader: "Valueset Name",
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
/* END OF FAUX DATA AND STUFF TO BE REMOVED WHEN IMPLEMENTING THE API */

const useValueSet = (): ValueSet[] => {
    const { authState } = useOktaAuth();
    const client = `${getStoredOrg()}.${getStoredSenderName()}`;

    const [sampleValueSetArray, setSampleValueSetArray] = useState<ValueSet[]>(
        []
    );

    let GetLatestVersion = async function GetLatestVersion() {
        let textBody;
        let response;
        try {
            response = await fetch(
                `${process.env.REACT_APP_BACKEND_URL}/api/lookuptables/list?showInactive=true`,
                {
                    method: "GET",
                    headers: {
                        "Content-Type": "text/csv",
                        client: client, // ignore.ignore-waters
                        "authentication-type": "okta",
                        Authorization: `Bearer ${authState?.accessToken?.accessToken}`,
                        // payloadName: fileName, // header Naming-Convention or namingConvention or naming-convention?
                    },
                }
            );

            textBody = await response.text();

            // if this JSON.parse fails, the body was most likely an error string from the server
            let body = JSON.parse(textBody);
            let filteredBody = body.filter(
                (tv: { tableName: string; isActive: boolean }) =>
                    tv.tableName === "sender_automation_value_set" &&
                    tv.isActive
            );
            if (filteredBody.length === 0) {
                filteredBody = body.filter(
                    (tv: { tableName: string }) =>
                        tv.tableName === "sender_automation_value_set"
                );
            }
            filteredBody = filteredBody.sort(
                (a: { [x: string]: number }, b: { [x: string]: number }) =>
                    b["tableVersion"] - a["tableVersion"]
            )[0];

            return filteredBody?.tableVersion;
        } catch (error) {
            return {
                ok: false,
                status: response ? response.status : 500,
                errors: [
                    {
                        details: textBody ? textBody : error,
                    },
                ],
            };
        }
    };
    let GetLatestData = async function GetLatestData(version: number) {
        let textBody;
        let response;
        try {
            response = await fetch(
                `${process.env.REACT_APP_BACKEND_URL}/api/lookuptables/sender_automation_value_set/${version}/content`,
                {
                    method: "GET",
                    headers: {
                        "Content-Type": "text/csv",
                        client: client, // ignore.ignore-waters
                        "authentication-type": "okta",
                        Authorization: `Bearer ${authState?.accessToken?.accessToken}`,
                        // payloadName: fileName, // header Naming-Convention or namingConvention or naming-convention?
                    },
                }
            );

            textBody = await response.text();

            return JSON.parse(textBody);
        } catch (error) {
            return {
                ok: false,
                status: response ? response.status : 500,
                errors: [
                    {
                        details: textBody ? textBody : error,
                    },
                ],
            };
        }
    };

    const getSenderAutomationData = async (): Promise<ValueSet[]> => {
        const version = await GetLatestVersion();
        if (version === undefined) {
            console.error("DANGER! no version was found");
            return [];
        }
        const data = await GetLatestData(version);

        return data.map(
            (set: {
                name: string;
                system: string;
                createdBy: string;
                createdAt: string;
            }) => ({
                name: set.name,
                system: set.system,
                createdBy: set.createdBy,
                createdAt: set.createdAt,
            })
        );
    };

    useEffect(() => {
        getSenderAutomationData().then((results) => {
            setSampleValueSetArray(results);
        });
    });

    return sampleValueSetArray;
};

const ValueSetsTable = () => {
    const sampleValueSetArray = useValueSet();

    const [valueSet, setValueSet] = useState<ValueSet[]>(sampleValueSetArray);
    console.log(valueSet);
    /* We'd pass our config and our API response in this */
    const tableConfig: TableConfig = {
        columns: sampleValueSetColumnConfig,
        rows: sampleValueSetArray,
    };
    /* These items, I'm assuming, are likely to be generated from API response data? */
    const legendItems: LegendItem[] = [
        { label: "Name", value: "HL00005" },
        { label: "Version", value: "2.5.1" },
        { label: "System", value: "HL7" },
        { label: "Reference", value: "Make this linkable" },
    ];
    /* We make this action do what we need it to to add an item */
    const datasetActionItem: DatasetAction = {
        label: "Add item",
        method: async () =>
            setValueSet([
                ...sampleValueSetArray,
                {
                    name: "",
                    system: "",
                    createdAt: "",
                    createdBy: "",
                },
            ]),
    };
    return (
        <Table
            title="ReportStream Value Sets"
            legend={<Legend items={legendItems} />}
            datasetAction={datasetActionItem}
            config={tableConfig}
            enableEditableRows
            editableCallback={(v: string) => console.log(v)}
        />
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
