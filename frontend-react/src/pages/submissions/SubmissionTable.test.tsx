import { Fixture, MockResolver } from "@rest-hooks/test";
import { screen, within } from "@testing-library/react";
import { ReactElement } from "react";
import { CacheProvider } from "rest-hooks";

import SubmissionsResource from "../../resources/SubmissionsResource";
import { renderWithRouter } from "../../utils/CustomRenderUtils";
import {
    _exportForTesting,
    FeatureFlagName,
} from "../../pages/misc/FeatureFlags";

import SubmissionTable from "./SubmissionTable";

const { addFeatureFlag, removeFeatureFlag } = _exportForTesting;

// TODO: Move this to CustomRenderUtils.tsx once we stop mocking rest-hooks.
// MockResolver is the preferred method for testing components that use
// rest-hooks (see https://resthooks.io/docs/guides/unit-testing-components) but
// since it conflicts with our current approach to use jest.mock, this helper
// can't be in a shared location util we update existing tests.
// See https://github.com/CDCgov/prime-reportstream/issues/5623
const renderWithResolver = (ui: ReactElement, fixtures: Fixture[]) =>
    renderWithRouter(
        <CacheProvider>
            <MockResolver fixtures={fixtures}>{ui}</MockResolver>
        </CacheProvider>
    );

const fixtures: Fixture[] = [
    {
        endpoint: SubmissionsResource.list(),
        args: [
            {
                cursor: "3000-01-01T00:00:00.000Z",
                endCursor: "2000-01-01T00:00:00.000Z",
                pageSize: 11,
                sort: "DESC",
                showFailed: false,
            },
        ],
        error: false,
        response: [
            { submissionId: 0 },
            { submissionId: 1 },
        ] as SubmissionsResource[],
    },
];

describe("SubmissionTable", () => {
    test("renders a table with the returned resources", async () => {
        renderWithResolver(<SubmissionTable />, fixtures);

        const filter = await screen.findByTestId("filter-container");
        expect(filter).toBeInTheDocument();

        const rowGroups = screen.getAllByRole("rowgroup");
        expect(rowGroups).toHaveLength(2);
        const tBody = rowGroups[1];
        const rows = within(tBody).getAllByRole("row");
        expect(rows).toHaveLength(2);
    });

    describe("when the numbered pagination feature flag is on", () => {
        beforeAll(() => {
            addFeatureFlag(FeatureFlagName.NUMBERED_PAGINATION);
        });

        afterAll(() => {
            removeFeatureFlag(FeatureFlagName.NUMBERED_PAGINATION);
        });

        test("renders a placeholder", async () => {
            renderWithResolver(<SubmissionTable />, fixtures);

            const tk = await screen.findByText("TK");
            expect(tk).toBeInTheDocument();
        });
    });
});
