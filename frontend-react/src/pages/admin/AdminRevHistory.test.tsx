import React from "react";

import { SettingRevision } from "../../network/api/Organizations/SettingRevisions";
import { renderWithQueryProvider } from "../../utils/CustomRenderUtils";

import { _exportForTesting } from "./AdminRevHistory";

// <editor-fold defaultstate="collapsed" desc="mockData: SettingRevision[]">
const fakeRows: SettingRevision[] = [
    {
        id: 72,
        name: "ignore",
        version: 0,
        createdBy: "local@test.com",
        createdAt: "2022-05-25T15:36:03.652Z",
        isDeleted: false,
        isActive: false,
        settingJson:
            '{"name": "ignore", "filters": [{"topic": "covid-19", "qualityFilter": null, "routingFilter": null, "jurisdictionalFilter": ["matches(ordering_facility_state, IG)"], "processingModeFilter": null}], "description": "FOR TESTING ONLY", "jurisdiction": "FEDERAL"}',
    },
    {
        id: 327,
        name: "ignore",
        version: 1,
        createdBy: "local1@test.com",
        createdAt: "2022-09-13T22:05:28.537Z",
        isDeleted: false,
        isActive: false,
        settingJson:
            '{"name": "ignore", "filters": [{"topic": "covid-19", "qualityFilter": null, "routingFilter": null, "jurisdictionalFilter": ["matches(ordering_facility_state, IG)"], "processingModeFilter": null}], "version": 0, "createdAt": "2022-05-25T15:36:03.652Z", "createdBy": "local@test.com", "description": "FOR TESTING ONLY", "jurisdiction": "FEDERAL"}',
    },
    {
        id: 328,
        name: "ignore",
        version: 2,
        createdBy: "local2@test.com",
        createdAt: "2022-09-13T22:05:39.839Z",
        isDeleted: false,
        isActive: true,
        settingJson:
            '{"name": "ignore", "filters": [{"topic": "covid-19", "qualityFilter": null, "routingFilter": null, "jurisdictionalFilter": ["matches(ordering_facility_state, IG)"], "processingModeFilter": null}], "version": 1, "createdAt": "2022-09-13T22:05:28.537Z", "createdBy": "local1@cdc.gov", "description": "FOR TESTING ONLY", "jurisdiction": "FEDERAL"}',
    },
];
// </editor-fold>

const mockError = new Error();
let mockUseData = jest.fn();

// router path
jest.mock("react-router-dom", () => ({
    useParams: () => ({ org: "ignore", settingType: "organization" }),
}));

describe("AdminRevHistory", () => {
    beforeEach(() => {
        mockUseData = jest.fn(() => ({
            valueSetArray: fakeRows,
            error: null,
        }));
    });

    test("Renders with no errors", () => {
        // only render with query provider
        // eslint-disable-next-line react/jsx-pascal-case
        renderWithQueryProvider(<_exportForTesting.AdminRevHistory />);
        // const headers = screen.getAllByRole("columnheader");
        // const title = screen.getByText("ReportStream Core Values");
        // const datasetActionButton = screen.getByText("Add item");
        // const rows = screen.getAllByRole("row");
        //
        // expect(headers.length).toEqual(4);
        // expect(title).toBeInTheDocument();
        // expect(datasetActionButton).toBeInTheDocument();
        // expect(rows.length).toBe(3); // +1 for header
    });
});
