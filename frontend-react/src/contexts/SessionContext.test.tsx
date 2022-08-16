import { render, screen } from "@testing-library/react";
import { useContext } from "react";

import { MemberType } from "../hooks/UseOktaMemberships";

import { SessionContext } from "./SessionContext";

const TestComponent = () => {
    const { memberships, oktaToken } = useContext(SessionContext);
    return (
        <>
            <div>{memberships.state.active?.parsedName || ""}</div>
            <div>{memberships.state.active?.memberType || ""}</div>
            <div>{oktaToken?.accessToken || ""}</div>
        </>
    );
};

describe("SessionContext", () => {
    test("renders data as passed in value prop", async () => {
        render(
            <SessionContext.Provider
                value={{
                    oktaToken: { accessToken: "testToken" },
                    memberships: {
                        state: {
                            active: {
                                parsedName: "testOrg",
                                memberType: MemberType.SENDER,
                            },
                        },
                        dispatch: () => {},
                    },
                }}
            >
                <TestComponent />
            </SessionContext.Provider>
        );
        const orgDiv = await screen.findByText("testOrg");
        const senderDiv = await screen.findByText(MemberType.SENDER);
        const tokenDiv = await screen.findByText("testToken");
        expect(orgDiv).toBeInTheDocument();
        expect(senderDiv).toBeInTheDocument();
        expect(tokenDiv).toBeInTheDocument();
    });
});
