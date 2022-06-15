import { renderHook } from "@testing-library/react-hooks";
import { rest } from "msw";
import { setupServer } from "msw/node";

import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";
import {
    MembershipController,
    MembershipSettings,
} from "../UseOktaMemberships";
import { SessionController } from "../UseSessionStorage";
import { MyApi, MyApiItem } from "../../network/api/test-tools/MockApi";

import useEndpoint from "./UseApi";

const handlers = [
    /* Returns a list of two fake api items */
    rest.get(
        "https://test.prime.cdc.gov/api/test/test/:id",
        (req, res, ctx) => {
            return res(ctx.status(200), ctx.json(new MyApiItem("test")));
        }
    ),
];

/* TEST SERVER TO USE IN `.test.ts` FILES */
const testServer = setupServer(...handlers);

describe("useEndpoint", () => {
    beforeAll(() => testServer.listen());
    afterEach(() => testServer.resetHandlers());
    afterAll(() => testServer.close());
    test("", async () => {
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            memberships: {
                state: {
                    active: {
                        parsedName: "ORGANIZATION",
                    } as MembershipSettings,
                },
            } as MembershipController,
            store: {} as SessionController,
        });
        const { result, waitForNextUpdate } = renderHook(() =>
            useEndpoint<{ id: number }, MyApiItem>(MyApi, "itemById", "GET", {
                id: 123,
            })
        );
        expect(result.current.loading).toEqual(true);
        await waitForNextUpdate();
        expect(result.current.data).toEqual([new MyApiItem("test")]);
    });
});
