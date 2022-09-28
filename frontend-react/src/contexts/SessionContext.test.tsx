import { render, screen } from "@testing-library/react";
import { useContext } from "react";

import { MemberType } from "../hooks/UseOktaMemberships";

import { SessionContext } from "./SessionContext";

const TestComponent = () => {
    const { activeMembership, oktaToken, initialized } =
        useContext(SessionContext);
    return (
        <>
            <div>{activeMembership?.parsedName || ""}</div>
            <div>{activeMembership?.memberType || ""}</div>
            <div>{oktaToken?.accessToken || ""}</div>
            <div>{initialized ? "INITIALIZED" : "NOT INITIALIZED"}</div>
        </>
    );
};

describe("SessionContext", () => {
    test("renders data as passed in value prop", async () => {
        render(
            <SessionContext.Provider
                value={{
                    oktaToken: { accessToken: "testToken" },
                    activeMembership: {
                        parsedName: "testOrg",
                        memberType: MemberType.SENDER,
                    },
                    dispatch: () => {},
                    initialized: true,
                }}
            >
                <TestComponent />
            </SessionContext.Provider>
        );
        const orgDiv = await screen.findByText("testOrg");
        const senderDiv = await screen.findByText(MemberType.SENDER);
        const tokenDiv = await screen.findByText("testToken");
        const initializedDiv = await screen.findByText("INITIALIZED");
        expect(orgDiv).toBeInTheDocument();
        expect(senderDiv).toBeInTheDocument();
        expect(tokenDiv).toBeInTheDocument();
        expect(initializedDiv).toBeInTheDocument();
    });
});
