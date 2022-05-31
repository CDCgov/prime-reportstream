import { fireEvent, screen } from "@testing-library/react";
import { IOktaContext } from "@okta/okta-react/bundles/types/OktaContext";

import ReportResource from "../../../resources/ReportResource";
import { renderWithRouter } from "../../../utils/CustomRenderUtils";
import * as SessionStorageTools from "../../../contexts/SessionStorageTools";
import { historyServer } from "../../../__mocks__/HistoryMockServer";

import ReportsTable from "./ReportsTable";
import * as ReportUtilsModule from "./ReportsUtils";

const mockMs = (additional?: number) =>
    additional ? 1652458218417 + additional : 1652458218417;
const mockAPIData: ReportResource[] = [
    new ReportResource("1", mockMs(), mockMs(100000), 99, "CSV"),
];
const mockFetchReport = jest
    .spyOn(ReportUtilsModule, "getReportAndDownload")
    .mockImplementation(() => {
        return mockAPIData[0];
    });
// @ts-ignore so we can mock this partially
const mockAuth = jest.fn<() => Partial<IOktaContext>>().mockReturnValue({
    authState: {
        accessToken: {
            accessToken: "",
        },
    },
});
jest.mock("@okta/okta-react", () => ({
    useOktaAuth: () => mockAuth(),
}));
const mockStoredOrg = jest.spyOn(SessionStorageTools, "getStoredOrg");
jest.mock("rest-hooks", () => ({
    useResource: () => {
        return mockAPIData;
    },
}));

describe("ReportsTable", () => {
    beforeAll(() => historyServer.listen());
    afterEach(() => historyServer.resetHandlers());
    afterAll(() => historyServer.close());
    beforeEach(() => renderWithRouter(<ReportsTable />));
    test("renders with no error", () => {
        // Hooks run
        expect(mockAuth).toHaveBeenCalled();
        expect(mockStoredOrg).toHaveBeenCalled();

        // Column headers render
        expect(screen.getByText("Report ID")).toBeInTheDocument();
        expect(screen.getByText("Date Sent")).toBeInTheDocument();
        expect(screen.getByText("Expires")).toBeInTheDocument();
        expect(screen.getByText("Total Tests")).toBeInTheDocument();
        expect(screen.getByText("File")).toBeInTheDocument();

        // Content loads
        expect(screen.getAllByRole("row")).toHaveLength(2);
    });

    test("dates are transformed on render", () => {
        const mockSent = new Date(mockMs()).toLocaleString();
        const mockExpires = new Date(mockMs()).toLocaleString();
        expect(screen.getByText(mockSent)).toBeInTheDocument();
        expect(screen.getByText(mockExpires)).toBeInTheDocument();
    });

    test("file button downloads file", () => {
        fireEvent.click(screen.getByText("CSV"));
        expect(mockFetchReport).toHaveBeenCalled();
    });
});
