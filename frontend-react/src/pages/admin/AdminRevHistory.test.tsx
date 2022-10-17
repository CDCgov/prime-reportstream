import React from "react";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderWithQueryProvider } from "../../utils/CustomRenderUtils";
import {
    SettingRevision,
    SettingRevisionParams,
} from "../../network/api/Organizations/SettingRevisions";

import { _exportForTesting } from "./AdminRevHistory";

// <editor-fold defaultstate="collapsed" desc="mockData: SettingRevision[]">
const fakeRows: SettingRevision[] = [
    {
        id: 72,
        name: "ignore",
        version: 0,
        createdBy: "local@test.com",
        createdAt: "2022-05-25T15:36:03.652Z",
        isDeleted: true,
        isActive: false,
        settingJson:
            '{"name": "ignore", "filters": [{"topic": "covid-19", "qualityFilter": null, "routingFilter": null, "jurisdictionalFilter": ["matches(ordering_facility_state, IG)"], "processingModeFilter": null}], "description": "ORIGINAL", "jurisdiction": "FEDERAL"}',
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
            '{"name": "ignore", "filters": [{"topic": "covid-19", "qualityFilter": null, "routingFilter": null, "jurisdictionalFilter": ["matches(ordering_facility_state, IG)"], "processingModeFilter": null}], "version": 0, "createdAt": "2022-05-25T15:36:03.652Z", "createdBy": "local@test.com", "description": "FIRST_REVISION", "jurisdiction": "FEDERAL"}',
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
            '{"name": "ignore", "filters": [{"topic": "covid-19", "qualityFilter": null, "routingFilter": null, "jurisdictionalFilter": ["matches(ordering_facility_state, IG)"], "processingModeFilter": null}], "version": 1, "createdAt": "2022-09-13T22:05:28.537Z", "createdBy": "local1@cdc.gov", "description": "3RD_EDIT", "jurisdiction": "FEDERAL"}',
    },
];
// </editor-fold>

// router path
jest.mock("react-router-dom", () => ({
    useParams: () => ({ org: "ignore", settingType: "organization" }),
}));

// replace this call to return our mock data
jest.mock("../../network/api/Organizations/SettingRevisions", () => {
    return {
        useSettingRevisionEndpointsQuery: (_params: SettingRevisionParams) => {
            // The results set (data, isLoading, error) needs to match what the component
            // expects to get back from the call to useSettingRevisionEndpointsQuery()
            return {
                data: fakeRows,
                isError: false,
                isLoading: false,
            };
        },
    };
});

describe("AdminRevHistory", () => {
    test("Renders with no errors", () => {
        // a REAL test would need Cypress to click revisions in the top two accordian lists
        // and verify the diffs are rendering the diffs correctly

        // eslint-disable-next-line react/jsx-pascal-case
        renderWithQueryProvider(<_exportForTesting.AdminRevHistory />);
        // useful: https://testing-library.com/docs/queries/about/
        // we expect 2x because of the right and left list layout
        // eslint-disable-next-line no-restricted-globals
        expect(screen.getAllByText(/local@test.com/).length).toBe(2);

        // click an item in each list and make sure the diff loads. (click parent row)
        {
            const clickTarget1 = screen.getAllByText(/local@test.com/)[0];
            const parentRow1 = clickTarget1.parentElement;
            expect(parentRow1).not.toBeNull();
            // key linter happy
            if (parentRow1 !== null) {
                userEvent.click(parentRow1);
            }
        }

        {
            const clickTarget2 = screen.getAllByText(/local1@test.com/)[1];
            const parentRow2 = clickTarget2.parentElement;
            expect(parentRow2).not.toBeNull();
            // key linter happy
            if (parentRow2 !== null) {
                userEvent.click(parentRow2);
            }
        }

        // make sure the meta data at the bottom is updated.
        {
            const leftMetaText =
                screen.getByTestId("meta-left-data").textContent;
            expect(leftMetaText).toBe("Flags: isDeleted: true isActive: false");
        }
        {
            const rightMetaText =
                screen.getByTestId("meta-right-data").textContent;
            expect(rightMetaText).toBe(
                "Flags: isDeleted: false isActive: false"
            );
        }

        // look for the unique "Description" text in each diff.
        {
            const leftDiffText =
                screen.getByTestId("left-compare-text").textContent || "";
            expect(/ORIGINAL/.test(leftDiffText)).toBe(true);
            expect(/FIRST_REVISION/.test(leftDiffText)).toBe(false);
        }
        {
            const rightDiffText =
                screen.getByTestId("right-compare-text").textContent || "";
            expect(/ORIGINAL/.test(rightDiffText)).toBe(false);
            expect(/FIRST_REVISION/.test(rightDiffText)).toBe(true);
        }
    });
});
