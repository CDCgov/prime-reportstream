import { screen } from "@testing-library/react";
import * as OktaReact from "@okta/okta-react";

import { renderApp } from "../../../utils/CustomRenderUtils";
import { FileType } from "../../../utils/TemporarySettingsAPITypes";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../../hooks/UseOktaMemberships";
import { AccessTokenWithRSClaims } from "../../../utils/OrganizationUtils";
import { formatDateWithoutSeconds } from "../../../utils/DateTimeUtils";

import { ReportDetailsSummary } from "./ReportDetailsSummary";

const mockAuth = jest.spyOn(OktaReact, "useOktaAuth");
const mockGetUser = jest.fn();

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
                report={{ ...DEFAULT_RSDELIVERY, expires: pastDate.toString() }}
            />,
        );

        expect(screen.queryByText(/Download as/)).not.toBeInTheDocument();
    });
});
