import { screen } from "@testing-library/react";

import { render } from "../../utils/Test/render";

import ReportStreamHeader from "./ReportStreamHeader";

vi.mock("../hooks/UseSenderResource");
vi.mock("../../hooks/UseOrganizationSettings", async (imp) => ({
    ...(await imp<typeof import("../../hooks/UseOrganizationSettings")>()),
    useOrganizationSettings__: vi.fn(() => ({
        data: {},
    })),
}));

describe("SignInOrUser", () => {
    // Every set of users should have access to the following Navbar items
    afterEach(() => {
        expect(screen.getByText("Getting started")).toBeVisible();
        expect(screen.getByText("Developers")).toBeVisible();
        expect(screen.getByText("Your connection")).toBeVisible();
        expect(screen.getByText("Support")).toBeVisible();
        expect(screen.getByText("About")).toBeVisible();
    });

    test("renders Sender permissioned ReportStreamNavbar", () => {
        render(<ReportStreamHeader />, {
            providers: {
                Session: {
                    config: {
                        IS_PREVIEW: false,
                    },
                    user: { isUserSender: true },
                },
            },
        });
        expect(screen.getByText("Submissions")).toBeVisible();
        expect(screen.queryByText("Dashboard")).not.toBeInTheDocument();
        expect(screen.queryByText("Admin")).not.toBeInTheDocument();
    });

    test("renders Receiver permissioned ReportStreamNavbar", () => {
        render(<ReportStreamHeader />, {
            providers: {
                Session: {
                    config: {
                        IS_PREVIEW: false,
                    },
                    user: { isUserReceiver: true },
                },
            },
        });
        expect(screen.queryByText("Submissions")).not.toBeInTheDocument();
        expect(screen.getByText("Dashboard")).toBeVisible();
        expect(screen.queryByText("Admin")).not.toBeInTheDocument();
    });

    test("renders Receiver AND Sender (Transceiver) permissioned ReportStreamNavbar", () => {
        render(<ReportStreamHeader />, {
            providers: {
                Session: {
                    config: {
                        IS_PREVIEW: false,
                    },
                    user: { isUserTransceiver: true },
                },
            },
        });
        expect(screen.getByText("Submissions")).toBeVisible();
        expect(screen.getByText("Dashboard")).toBeVisible();
        expect(screen.queryByText("Admin")).not.toBeInTheDocument();
    });

    test("renders Admin permissioned ReportStreamNavbar", () => {
        render(<ReportStreamHeader />, {
            providers: {
                Session: {
                    config: {
                        IS_PREVIEW: false,
                    },
                    user: { isUserAdmin: true, isAdminStrictCheck: true },
                },
            },
        });
        expect(screen.getByText("Submissions")).toBeVisible();
        expect(screen.getByText("Dashboard")).toBeVisible();
        expect(screen.getByText("Admin")).toBeVisible();
    });
});
