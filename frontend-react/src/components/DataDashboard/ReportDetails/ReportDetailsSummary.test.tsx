import { screen } from "@testing-library/react";

import { ReportDetailsSummary } from "./ReportDetailsSummary";
import { renderApp } from "../../../utils/CustomRenderUtils";
import { formatDateWithoutSeconds } from "../../../utils/DateTimeUtils";
import {
    AccessTokenWithRSClaims,
    MemberType,
} from "../../../utils/OrganizationUtils";
import { FileType } from "../../../utils/TemporarySettingsAPITypes";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../../contexts/Session/__mocks__/useSessionContext")
>("../../../contexts/Session/useSessionContext");
const mockGetUser = vi.fn();

const currentDate = new Date();
const futureDate = new Date(currentDate.setDate(currentDate.getDate() + 1));
const pastDate = new Date(currentDate.setDate(currentDate.getDate() - 1));

const DEFAULT_RSDELIVERY = {
    deliveryId: 1,
    batchReadyAt: "2022-09-28T22:21:33.801667",
    expires: futureDate.toString(),
    receiver: "elr",
    reportId: "123",
    topic: "covid-19",
    reportItemCount: 10,
    fileName: "I Am A File",
    fileType: FileType.CSV,
};

beforeEach(() => {
    // Mock our SessionProvider's data
    mockSessionContentReturnValue({
        oktaAuth: {
            getUser: mockGetUser.mockResolvedValue({
                email: "test@test.org",
            }),
        } as any,
        authState: {
            isAuthenticated: true,
            accessToken: {
                accessToken: "TOKEN",
                claims: {
                    organization: ["Test-Org"],
                },
            } as AccessTokenWithRSClaims,
        },
        activeMembership: {
            memberType: MemberType.RECEIVER,
            parsedName: "testOrg",
            service: "testReceiverService",
        },

        user: {
            isUserAdmin: false,
            isUserReceiver: true,
            isUserSender: false,
            isUserTransceiver: false,
        } as any,
    });
});

describe("ReportDetailsSummary", () => {
    test("renders expected content", () => {
        const expectedExpireDate = formatDateWithoutSeconds(
            futureDate.toString(),
        );
        renderApp(<ReportDetailsSummary report={DEFAULT_RSDELIVERY} />);

        expect(screen.getByText(/Download as/)).toBeVisible();
        expect(screen.queryByText("CSV")).toBeVisible();

        expect(screen.getByText(/Report ID/)).toBeVisible();
        expect(screen.getByText(/123/)).toBeVisible();
        expect(screen.getByText(/Delivery method/)).toBeVisible();
        expect(screen.getByText(/SFTP/)).toBeVisible();
        expect(screen.getByText(/Date sent to you/)).toBeVisible();
        expect(screen.getByText("9/28/2022 10:21 PM")).toBeVisible();
        expect(screen.getByText(/Available until/)).toBeVisible();
        expect(screen.getByText(expectedExpireDate)).toBeVisible();
    });

    test("Does not display the download button if the date has expired", () => {
        renderApp(
            <ReportDetailsSummary
                report={{
                    ...DEFAULT_RSDELIVERY,
                    expires: pastDate.toISOString(),
                }}
            />,
        );

        expect(screen.queryByText(/Download as/)).not.toBeInTheDocument();
    });
});
