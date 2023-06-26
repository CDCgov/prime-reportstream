import React from "react";

import {
    MarkdownLayout,
    MarkdownLayoutProps,
} from "../../../../components/Content/MarkdownLayout";
import { USSmartLink } from "../../../../components/USLink";
import Markdown from "../../../../content/resources/reportstream-api/documentation/data-model/DataModel.mdx";
import PatientDataElementsTable from "../../../../content/resources/reportstream-api/documentation/data-model/PatientDataElementsTable.mdx";
import OrderAndResultDataElementsTable from "../../../../content/resources/reportstream-api/documentation/data-model/OrderAndResultDataElementsTable.mdx";
import SpecimenDataElementsTable from "../../../../content/resources/reportstream-api/documentation/data-model/SpecimenDataElementsTable.mdx";
import OrderingProviderDataElementsTable from "../../../../content/resources/reportstream-api/documentation/data-model/OrderingProviderDataElementsTable.mdx";
import TestingFacilityDataElementsTable from "../../../../content/resources/reportstream-api/documentation/data-model/TestingFacilityDataElementsTable.mdx";
import AskOnEntryTable from "../../../../content/resources/reportstream-api/documentation/data-model/AskOnEntryTable.mdx";
import ReportingAndOrderingFacilityDataElementsTable from "../../../../content/resources/reportstream-api/documentation/data-model/ReportingAndOrderingFacilityDataElementsTable.mdx";
import Sidenav from "../../../../content/resources/reportstream-api/Sidenav.mdx";

export interface DataModelPageProps extends MarkdownLayoutProps {}

export function DataModelPage(props: DataModelPageProps) {
    const mdx: typeof props.mdx = {
        components: {
            a: USSmartLink,
            AskOnEntryTable,
            OrderAndResultDataElementsTable,
            OrderingProviderDataElementsTable,
            PatientDataElementsTable,
            SpecimenDataElementsTable,
            TestingFacilityDataElementsTable,
            ReportingAndOrderingFacilityDataElementsTable,
        },
    };
    return (
        <MarkdownLayout sidenav={<Sidenav />} mdx={mdx} {...props}>
            <Markdown />
        </MarkdownLayout>
    );
}

export default DataModelPage;
