import { screen } from "@testing-library/react";

import { render } from "../../utils/Test/render";
import { RSUser } from "../../utils/OrganizationUtils";

import { ReportStreamNavbarBase } from "./ReportStreamNavbar";

vi.mock("../hooks/UseSenderResource");
vi.mock("../../hooks/UseOrganizationSettings", async (imp) => ({
    ...(await imp<typeof import("../../hooks/UseOrganizationSettings")>()),
    useOrganizationSettings__: vi.fn(() => ({
        data: {},
    })),
}));

describe("ReportStreamNavbar", () => {
    const mockOnLogout = vi.fn();
    const mockOnClearImpersonation = vi.fn();

    // Every set of users should have access to the following Navbar items
    afterEach(() => {
        expect(
            screen.getByRole("link", { name: "Getting started" }),
        ).toBeInTheDocument();
        expect(
            screen.getByRole("link", { name: "Developers" }),
        ).toBeInTheDocument();
        expect(
            screen.getByRole("link", { name: "Your connection" }),
        ).toBeInTheDocument();
        expect(
            screen.getByRole("link", { name: "Support" }),
        ).toBeInTheDocument();
        expect(
            screen.getByRole("button", { name: "About" }),
        ).toBeInTheDocument();
    });

    test("renders Sender permissioned Navbar", () => {
        render(
            <ReportStreamNavbarBase
                user={
                    new RSUser({
                        claims: { organization: ["DHSender_Test"] } as any,
                    })
                }
                contactUsUrl=""
                onClearImpersonation={mockOnClearImpersonation}
                onLogout={mockOnLogout}
            />,
        );

        expect(
            screen.getByRole("link", { name: "Submissions" }),
        ).toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Dashboard" })).toBeNull();
        expect(screen.queryByRole("button", { name: "Admin" })).toBeNull();
    });

    test("renders Receiver permissioned Navbar", () => {
        render(
            <ReportStreamNavbarBase
                user={
                    new RSUser({
                        claims: { organization: ["DHTest"] } as any,
                    })
                }
                contactUsUrl=""
                onClearImpersonation={mockOnClearImpersonation}
                onLogout={mockOnLogout}
            />,
        );

        expect(screen.queryByRole("link", { name: "Submissions" })).toBeNull();
        expect(
            screen.getByRole("link", { name: "Dashboard" }),
        ).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Admin" })).toBeNull();
    });

    test("renders Receiver AND Sender (Transceiver) permissioned ReportStreamNavbar", () => {
        render(
            <ReportStreamNavbarBase
                user={
                    new RSUser({
                        claims: {
                            organization: ["DHTest", "DHSender_Test"],
                        } as any,
                    })
                }
                contactUsUrl=""
                onClearImpersonation={mockOnClearImpersonation}
                onLogout={mockOnLogout}
            />,
        );

        expect(
            screen.getByRole("link", { name: "Submissions" }),
        ).toBeInTheDocument();
        expect(
            screen.getByRole("link", { name: "Dashboard" }),
        ).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Admin" })).toBeNull();
    });

    test("renders Admin permissioned Navbar", () => {
        render(
            <ReportStreamNavbarBase
                user={
                    new RSUser({
                        claims: { organization: ["DHPrimeAdmins"] } as any,
                    })
                }
                contactUsUrl=""
                onClearImpersonation={mockOnClearImpersonation}
                onLogout={mockOnLogout}
            />,
        );
        expect(screen.queryByRole("link", { name: "Submissions" })).toBeNull();
        expect(screen.queryByRole("link", { name: "Dashboard" })).toBeNull();
        expect(
            screen.getByRole("button", { name: "Admin" }),
        ).toBeInTheDocument();
    });
});
