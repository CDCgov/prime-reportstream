import { screen, waitFor } from "@testing-library/react";

import ReportStreamHeader from "./ReportStreamHeader";
import { RSSessionContext } from "../../contexts/Session/SessionProvider";
import useSessionContext from "../../contexts/Session/useSessionContext";
import { Organizations } from "../../hooks/UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";
import { renderApp } from "../../utils/CustomRenderUtils";
import { MemberType } from "../../utils/OrganizationUtils";

const mockUseSessionContext = vi.mocked(useSessionContext);

describe("SignInOrUser", () => {
    // Every set of users should have access to the following Navbar items
    function testNav() {
        expect(screen.getByText("Getting started")).toBeVisible();
        expect(screen.getByText("Developers")).toBeVisible();
        expect(screen.getByText("Your connection")).toBeVisible();
        expect(screen.getByText("Support")).toBeVisible();
        expect(screen.getByText("About")).toBeVisible();
    }

    test("renders Sender permission ReportStreamNavbar", async () => {
        mockUseSessionContext.mockReturnValue({
            config: {
                IS_PREVIEW: false,
            },
            user: { isUserSender: true },
            activeMembership: {
                parsedName: "ignore",
                memberType: MemberType.SENDER,
                service: "default",
            },
        } as RSSessionContext);
        renderApp(<ReportStreamHeader />);
        await waitFor(() => expect(screen.getByText("Submission History")).toBeVisible());
        expect(screen.queryByText("Admin tools")).not.toBeInTheDocument();
        testNav();
    });

    test("renders Receiver permission ReportStreamNavbar", async () => {
        mockUseSessionContext.mockReturnValue({
            config: {
                IS_PREVIEW: false,
            },
            user: { isUserReceiver: true },
            activeMembership: {
                parsedName: "ak-phd",
                memberType: MemberType.RECEIVER,
                service: undefined,
            },
        } as RSSessionContext);
        renderApp(<ReportStreamHeader />);
        await waitFor(() => expect(screen.getByText("Daily Data")).toBeVisible());
        expect(screen.queryByText("Submission History")).not.toBeInTheDocument();
        expect(screen.queryByText("Admin tools")).not.toBeInTheDocument();
        expect(screen.getByText("Log out")).toBeVisible();
        testNav();
    });

    test("renders Receiver AND Sender (Transceiver) permission ReportStreamNavbar", async () => {
        mockUseSessionContext.mockReturnValue({
            config: {
                IS_PREVIEW: false,
            },
            user: { isUserTransceiver: true },
            activeMembership: {
                parsedName: "ak-phd",
                memberType: (MemberType.SENDER, MemberType.RECEIVER),
                service: undefined,
            },
        } as RSSessionContext);
        renderApp(<ReportStreamHeader />);
        await waitFor(() => expect(screen.getByText("Daily Data")).toBeVisible());
        expect(screen.getByText("Submission History")).toBeVisible();
        expect(screen.getByText("Log out")).toBeVisible();
        expect(screen.queryByText("Admin tools")).not.toBeInTheDocument();
        testNav();
    });

    test("renders Admin permission ReportStreamNavbar", async () => {
        mockUseSessionContext.mockReturnValue({
            config: {
                IS_PREVIEW: false,
            },
            user: { isUserAdmin: true, isAdminStrictCheck: true },
            activeMembership: {
                memberType: MemberType.PRIME_ADMIN,
                parsedName: Organizations.PRIMEADMINS,
            },
        } as RSSessionContext);
        renderApp(<ReportStreamHeader />);
        await waitFor(() => expect(screen.getByText("Submission History")).toBeVisible());
        expect(screen.getByText("Daily Data")).toBeVisible();
        expect(screen.getByText("Admin tools")).toBeVisible();
        expect(screen.getByText("Log out")).toBeVisible();
        testNav();
    });

    test("renders logout when no member type permission", async () => {
        mockUseSessionContext.mockReturnValue({
            config: {
                IS_PREVIEW: false,
            },
            user: {},
            activeMembership: {},
        } as RSSessionContext);
        renderApp(<ReportStreamHeader />);
        await waitFor(() => expect(screen.getByText("Log out")).toBeVisible());
        expect(screen.queryByText("Submission History")).not.toBeInTheDocument();
        expect(screen.queryByText("Daily Data")).not.toBeInTheDocument();
        expect(screen.queryByText("Admin tools")).not.toBeInTheDocument();
        testNav();
    });

    test("renders SunsetNoticeBanner on all pages", async () => {
        mockUseSessionContext.mockReturnValue({
            config: { IS_PREVIEW: false },
            user: {},
        } as RSSessionContext);

        renderApp(<ReportStreamHeader />);

        await waitFor(() => expect(screen.getByText("ReportStream Sunset Notice")).toBeVisible());
        expect(screen.getByText(/December 31, 2025/)).toBeVisible();
    });
});
