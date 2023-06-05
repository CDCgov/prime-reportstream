import { ComponentMeta, ComponentStoryObj } from "@storybook/react";
import React from "react";

import { USSmartLink } from "../../../../components/USLink";
import PatientDataElementsTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/PatientDataElementsTable.mdx";
import OrderAndResultDataElementsTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/OrderAndResultDataElementsTable.mdx";
import SpecimenDataElementsTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/SpecimenDataElementsTable.mdx";
import OrderingProviderDataElementsTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/OrderingProviderDataElementsTable.mdx";
import TestingFacilityDataElementsTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/TestingFacilityDataElementsTable.mdx";
import AskOnEntryTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/AskOnEntryTable.mdx";
import ReportingAndOrderingFacilityDataElementsTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/ReportingAndOrderingFacilityDataElementsTable.mdx";
import { MarkdownLayout } from "../../../../components/Content/MarkdownLayout";

import DataModelPage from "./DataModel";

export default {
    title: "pages/resources/api/documentation/DataModel",
    component: DataModelPage,
} as ComponentMeta<typeof DataModelPage>;

export const Default: ComponentStoryObj<typeof DataModelPage> = {
    args: {
        mdx: {
            components: {
                a: USSmartLink,
                AskOnEntryTable: React.Fragment,
                OrderAndResultDataElementsTable: React.Fragment,
                OrderingProviderDataElementsTable: React.Fragment,
                PatientDataElementsTable: React.Fragment,
                SpecimenDataElementsTable: React.Fragment,
                TestingFacilityDataElementsTable: React.Fragment,
                ReportingAndOrderingFacilityDataElementsTable: React.Fragment,
            },
        },
    },
};

export const PatientDataElementsTable: ComponentStoryObj<
    typeof PatientDataElementsTableMD
> = {
    render: () => (
        <MarkdownLayout sidenav={<></>}>
            <PatientDataElementsTableMD />
        </MarkdownLayout>
    ),
};

export const AskOnEntryTable: ComponentStoryObj<typeof AskOnEntryTableMD> = {
    render: () => (
        <MarkdownLayout sidenav={<></>}>
            <AskOnEntryTableMD />
        </MarkdownLayout>
    ),
};

export const OrderAndResultDataElementsTable: ComponentStoryObj<
    typeof OrderAndResultDataElementsTableMD
> = {
    render: () => (
        <MarkdownLayout sidenav={<></>}>
            <OrderAndResultDataElementsTableMD />
        </MarkdownLayout>
    ),
};

export const OrderingProviderDataElementsTable: ComponentStoryObj<
    typeof OrderingProviderDataElementsTableMD
> = {
    render: () => (
        <MarkdownLayout sidenav={<></>}>
            <OrderingProviderDataElementsTableMD />
        </MarkdownLayout>
    ),
};

export const SpecimenDataElementsTable: ComponentStoryObj<
    typeof SpecimenDataElementsTableMD
> = {
    render: () => (
        <MarkdownLayout sidenav={<></>}>
            <SpecimenDataElementsTableMD />
        </MarkdownLayout>
    ),
};

export const TestingFacilityDataElementsTable: ComponentStoryObj<
    typeof TestingFacilityDataElementsTableMD
> = {
    render: () => (
        <MarkdownLayout sidenav={<></>}>
            <TestingFacilityDataElementsTableMD />
        </MarkdownLayout>
    ),
};

export const ReportingAndOrderingFacilityDataElementsTable: ComponentStoryObj<
    typeof PatientDataElementsTableMD
> = {
    render: () => (
        <MarkdownLayout sidenav={<></>}>
            <ReportingAndOrderingFacilityDataElementsTableMD />
        </MarkdownLayout>
    ),
};
