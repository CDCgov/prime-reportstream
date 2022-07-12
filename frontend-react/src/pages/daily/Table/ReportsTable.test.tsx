import { fireEvent, screen } from "@testing-library/react";

import { renderWithRouter } from "../../../utils/CustomRenderUtils";
import { historyServer } from "../../../__mocks__/HistoryMockServer";
import * as ReportsHooks from "../../../hooks/network/History/ReportsHooks";
import { RSReportInterface } from "../../../network/api/History/Reports";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import {
    MembershipController,
    MemberType,
} from "../../../hooks/UseOktaMemberships";
import { SessionController } from "../../../hooks/UseSessionStorage";

import * as ReportUtilsModule from "./ReportsUtils";
import ReportsTable from "./ReportsTable";

const mockMs = (additional?: number) =>
    additional ? 1652458218417 + additional : 1652458218417;
const mockFetchReport = jest.spyOn(ReportUtilsModule, "getReportAndDownload");
const mockApiHook = jest.spyOn(ReportsHooks, "useReportsList");
const makeFakeData = (count: number) => {
    const data: RSReportInterface[] = [];
    for (count; count > 0; count--) {
        data.push({
            reportId: `${count}`,
            actions: [],
            content: "",
            displayName: "",
            facilities: [],
            fileName: "",
            mimeType: "",
            positive: 0,
            receivingOrg: "",
            receivingOrgSvc: "",
            sendingOrg: "",
            type: "",
            via: "",
            sent: mockMs(),
            expires: mockMs(100000),
            total: 99,
            fileType: "CSV",
        });
    }
    return data;
};

describe("ReportsTable", () => {
    beforeAll(() => historyServer.listen());
    afterEach(() => historyServer.resetHandlers());
    afterAll(() => historyServer.close());
    beforeEach(() => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.RECEIVER,
                        parsedName: "testOrg",
                        senderName: undefined,
                    },
                },
            } as MembershipController,
            store: {} as SessionController, // TS yells about removing this because of types
        });
        const reports = makeFakeData(199);
        mockApiHook.mockReturnValue({
            data: reports,
            loading: false,
            error: "",
            trigger: () => {},
        });
        renderWithRouter(<ReportsTable />);
    });
    test("renders with no error", async () => {
        // Column headers render
        expect(await screen.findByText("Report ID")).toBeInTheDocument();
        expect(await screen.findByText("Date Sent")).toBeInTheDocument();
        expect(await screen.findByText("Expires")).toBeInTheDocument();
        expect(await screen.findByText("Total Tests")).toBeInTheDocument();
        expect(await screen.findByText("File")).toBeInTheDocument();
    });

    test("renders 100 results per page + 1 header row", () => {
        const rows = screen.getAllByRole("row");
        expect(rows).toHaveLength(100 + 1);
    });

    test("dates are transformed on render", async () => {
        const mockSent = new Date(mockMs()).toLocaleString();
        const mockExpires = new Date(mockMs()).toLocaleString();
        const renderedSent = await screen.findAllByText(mockSent);
        const renderedExpires = await screen.findAllByText(mockExpires);
        expect(renderedSent).toHaveLength(100);
        expect(renderedExpires).toHaveLength(100);
    });

    test("file button downloads file", async () => {
        const mockReports = makeFakeData(1);
        mockFetchReport.mockReturnValue(mockReports[0]);
        const buttons = await screen.findAllByText("CSV");
        fireEvent.click(buttons[0]);
        expect(mockFetchReport).toHaveBeenCalled();
    });
});
