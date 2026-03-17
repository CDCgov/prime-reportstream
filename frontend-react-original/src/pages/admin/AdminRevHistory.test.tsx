import { screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";

import { _exportForTesting } from "./AdminRevHistory";
import { RSSettingRevision, RSSettingRevisionParams } from "../../hooks/api/UseSettingsRevisions/UseSettingsRevisions";
import { renderApp } from "../../utils/CustomRenderUtils";

const fakeRows: RSSettingRevision[] = [
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

// router path
vi.mock("react-router-dom", async (importActual) => ({
    ...(await importActual<typeof import("react-router-dom")>()),
    useParams: () => ({ org: "ignore", settingType: "organization" }),
}));

// replace this call to return our mock data
vi.mock("../../hooks/api/UseSettingsRevisions/UseSettingsRevisions", () => {
    return {
        default: (_params: RSSettingRevisionParams) => {
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
    test("Renders with no errors", async () => {
        // a REAL test would need Cypress to click revisions in the top two accordion lists
        // and verify the diffs are rendering the diffs correctly

        renderApp(<_exportForTesting.AdminRevHistory />);
        // useful: https://testing-library.com/docs/queries/about/
        // we expect 2x because of the right and left list layout

        expect(screen.getAllByText(/local@test.com/).length).toBe(2);

        // click an item in each list and make sure the diff loads. (click parent row)
        // revision-row[0..2] = left accordion, revision-row[3..5] = right accordion
        {
            const row1 = screen.getAllByTestId("revision-row")[0];
            expect(row1).toBeInTheDocument();
            await userEvent.click(row1);
        }

        {
            const row2 = screen.getAllByTestId("revision-row")[4];
            expect(row2).toBeInTheDocument();
            await userEvent.click(row2);
        }

        // make sure the meta data at the bottom is updated.
        {
            const leftMetaText = screen.getByTestId("meta-left-data").textContent;
            expect(leftMetaText).toBe("Flags: isDeleted: true isActive: false");
        }
        {
            const rightMetaText = screen.getByTestId("meta-right-data").textContent;
            expect(rightMetaText).toBe("Flags: isDeleted: false isActive: false");
        }

        // look for the unique "Description" text in each diff.
        {
            const leftDiffText = screen.getByTestId("left-compare-text").textContent ?? "";
            expect(leftDiffText.includes("ORIGINAL")).toBe(true);
            expect(leftDiffText.includes("FIRST_REVISION")).toBe(false);
        }
        {
            const rightDiffText = screen.getByTestId("right-compare-text").textContent ?? "";
            expect(rightDiffText.includes("ORIGINAL")).toBe(false);
            expect(rightDiffText.includes("FIRST_REVISION")).toBe(true);
        }
    });
});
