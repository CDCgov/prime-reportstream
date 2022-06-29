import React, { ReactElement, Suspense } from "react";
import { Fixture, MockResolver } from "@rest-hooks/test";
import { CacheProvider } from "rest-hooks";
import * as OktaReact from "@okta/okta-react";

import Spinner from "../components/Spinner";
import { renderWithSession } from "../utils/CustomRenderUtils";
import { mockSessionContext } from "../contexts/__mocks__/SessionContext";
import { MemberType } from "../hooks/UseOktaMemberships";

import Validate from "./Validate";

const mockAuth = jest.spyOn(OktaReact, "useOktaAuth");
const mockGetUser = jest.fn();

describe("Validate", () => {
    const renderWithResolver = (ui: ReactElement, fixtures: Fixture[]) =>
        renderWithSession(
            <CacheProvider>
                <MockResolver fixtures={fixtures}>{ui}</MockResolver>
            </CacheProvider>
        );

    test("Renders with no errors", () => {
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
                        //@ts-ignore
                        organization: ["DHPrimeAdmins"],
                    },
                },
            },
        });
        mockSessionContext.mockReturnValue({
            //@ts-ignore
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.PRIME_ADMIN,
                        parsedName: "PrimeAdmins",
                    },
                },
            },
        });
        renderWithResolver(
            <Suspense fallback={<Spinner size={"fullpage"} />}>
                <Validate />
            </Suspense>,
            []
        );
    });
});
