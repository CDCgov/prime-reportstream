import { screen } from "@testing-library/react";
import * as OktaReact from "@okta/okta-react";

import { renderApp } from "../../../utils/CustomRenderUtils";
import { FileType } from "../../../utils/TemporarySettingsAPITypes";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../../hooks/UseOktaMemberships";
import { AccessTokenWithRSClaims } from "../../../utils/OrganizationUtils";

import { ReportDetailsSummary } from "./ReportDetailsSummary";

const mockAuth = jest.spyOn(OktaReact, "useOktaAuth");
const mockGetUser = jest.fn();

const DEFAULT_RSDELIVERY = {
    deliveryId: 1,
    batchReadyAt: "2022-09-28T22:21:33.801667",
    expires: "2023-09-28T22:21:33.801667",
    receiver: "elr",
    reportId: "123",
    topic: "covid-19",
    reportItemCount: 10,
    fileName: "I Am A File",
    fileType: FileType.CSV,
};

beforeEach(() => {
    mockAuth.mockReturnValue({
        //@ts-ignore
        oktaAuth: {
            getUser: mockGetUser.mockResolvedValue({
                email: "test@test.org",
            }),
        },
        authState: {
            isAuthenticated: true,
            accessToken: {
                claims: {
                    organization: ["Test-Org"],
                },
            } as AccessTokenWithRSClaims,
        },
    });
    // Mock our SessionProvider's data
    mockSessionContext.mockReturnValue({
        oktaToken: {
            accessToken: "TOKEN",
        },
        activeMembership: {
            memberType: MemberType.RECEIVER,
            parsedName: "testOrg",
            service: "testReceiverService",
        },
        dispatch: () => {},
        initialized: true,
        isUserAdmin: false,
        isUserReceiver: true,
        isUserSender: false,
        environment: "test",
    });
});

describe("ReportDetailsSummary", () => {
    test("renders expected content", () => {
        renderApp(<ReportDetailsSummary report={DEFAULT_RSDELIVERY} />);

        expect(screen.getByText(/Report ID/)).toBeVisible();
        expect(screen.getByText(/123/)).toBeVisible();
        expect(screen.getByText(/Date range/)).toBeVisible();
        expect(
            screen.getByText("9/28/2022 10:21 PM - 9/28/2023 10:21 PM")
        ).toBeVisible();
        expect(screen.getByText(/Delivery method/)).toBeVisible();
        expect(screen.getByText(/SFTP/)).toBeVisible();
        expect(screen.getByText(/Date sent to you/)).toBeVisible();
        expect(screen.getByText("9/28/2022 10:21 PM")).toBeVisible();
        expect(screen.getByText(/Available until/)).toBeVisible();
        expect(screen.getByText("9/28/2023 10:21 PM")).toBeVisible();
    });
});
