import { ComponentMeta, ComponentStoryObj } from "@storybook/react";
import React from "react";

import { USSmartLink } from "../../../../components/USLink";
import PatientDataElementsTableMD from "../../../../content/resources/reportstream-api/documentation/data-model/PatientDataElementsTable.mdx";
import OrderAndResultDataElementsTableMD from "../../../../content/resources/reportstream-api/documentation/data-model/OrderAndResultDataElementsTable.mdx";
import SpecimenDataElementsTableMD from "../../../../content/resources/reportstream-api/documentation/data-model/SpecimenDataElementsTable.mdx";
import OrderingProviderDataElementsTableMD from "../../../../content/resources/reportstream-api/documentation/data-model/OrderingProviderDataElementsTable.mdx";
import TestingFacilityDataElementsTableMD from "../../../../content/resources/reportstream-api/documentation/data-model/TestingFacilityDataElementsTable.mdx";
import AskOnEntryTableMD from "../../../../content/resources/reportstream-api/documentation/data-model/AskOnEntryTable.mdx";
import ReportingAndOrderingFacilityDataElementsTableMD from "../../../../content/resources/reportstream-api/documentation/data-model/ReportingAndOrderingFacilityDataElementsTable.mdx";
import { MarkdownLayout } from "../../../../components/Content/MarkdownLayout";

import DataModelPage from "./DataModel";

export default {
    title: "pages/resources/api/documentation/DataModel",
    component: DataModelPage,
} as ComponentMeta<typeof DataModelPage>;

function ExternalStory() {
    return <>{"{OMITTED}"}</>;
}

export const Default: ComponentStoryObj<typeof DataModelPage> = {
    args: {
        mdx: {
            components: {
                a: USSmartLink,
                AskOnEntryTable: ExternalStory,
                OrderAndResultDataElementsTable: ExternalStory,
                OrderingProviderDataElementsTable: ExternalStory,
                PatientDataElementsTable: ExternalStory,
                SpecimenDataElementsTable: ExternalStory,
                TestingFacilityDataElementsTable: ExternalStory,
                ReportingAndOrderingFacilityDataElementsTable: ExternalStory,
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
