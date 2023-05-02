import { ComponentMeta, ComponentStoryObj } from "@storybook/react";
import React from "react";
import { Grid, GridContainer } from "@trussworks/react-uswds";

import { USSmartLink } from "../../../../components/USLink";
import PatientDataElementsTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/PatientDataElementsTable.mdx";
import OrderAndResultDataElementsTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/OrderAndResultDataElementsTable.mdx";
import SpecimenDataElementsTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/SpecimenDataElementsTable.mdx";
import OrderingProviderDataElementsTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/OrderingProviderDataElementsTable.mdx";
import TestingFacilityDataElementsTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/TestingFacilityDataElementsTable.mdx";
import AskOnEntryTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/AskOnEntryTable.mdx";
import ReportingAndOrderingFacilityDataElementsTableMD from "../../../../content/resources/api-programmers-guide/documentation/data-model/ReportingAndOrderingFacilityDataElementsTable.mdx";

import DataModelPage from "./DataModel";

export default {
    title: "pages/resources/documentation/DataModel",
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

const WithGrid = (props: React.PropsWithChildren<{}>) => {
    return (
        <GridContainer className="usa-prose">
            <Grid row className="flex-justify">
                <nav
                    aria-label="side-navigation"
                    className="tablet:grid-col-3"
                />
                <main className="tablet:grid-col-8">{props.children}</main>
            </Grid>
        </GridContainer>
    );
};

export const PatientDataElementsTable: ComponentStoryObj<
    typeof PatientDataElementsTableMD
> = {
    render: () => (
        <WithGrid>
            <PatientDataElementsTableMD />
        </WithGrid>
    ),
};

export const AskOnEntryTable: ComponentStoryObj<typeof AskOnEntryTableMD> = {
    render: () => (
        <WithGrid>
            <AskOnEntryTableMD />
        </WithGrid>
    ),
};

export const OrderAndResultDataElementsTable: ComponentStoryObj<
    typeof OrderAndResultDataElementsTableMD
> = {
    render: () => (
        <WithGrid>
            <OrderAndResultDataElementsTableMD />
        </WithGrid>
    ),
};

export const OrderingProviderDataElementsTable: ComponentStoryObj<
    typeof OrderingProviderDataElementsTableMD
> = {
    render: () => (
        <WithGrid>
            <OrderingProviderDataElementsTableMD />
        </WithGrid>
    ),
};

export const SpecimenDataElementsTable: ComponentStoryObj<
    typeof SpecimenDataElementsTableMD
> = {
    render: () => (
        <WithGrid>
            <SpecimenDataElementsTableMD />
        </WithGrid>
    ),
};

export const TestingFacilityDataElementsTable: ComponentStoryObj<
    typeof TestingFacilityDataElementsTableMD
> = {
    render: () => (
        <WithGrid>
            <TestingFacilityDataElementsTableMD />
        </WithGrid>
    ),
};

export const ReportingAndOrderingFacilityDataElementsTable: ComponentStoryObj<
    typeof PatientDataElementsTableMD
> = {
    render: () => (
        <WithGrid>
            <ReportingAndOrderingFacilityDataElementsTableMD />
        </WithGrid>
    ),
};
