import { act, renderHook } from "@testing-library/react-hooks";
import { rest } from "msw";
import { setupServer } from "msw/node";

import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";
import {
    MembershipController,
    MembershipSettings,
} from "../UseOktaMemberships";
import { SessionController } from "../UseSessionStorage";
import { MyApi, MyApiItem } from "../../network/api/test-tools/MockApi";
import { SimpleError } from "../../utils/UsefulTypes";

import useEndpoint, { passesObjCompare } from "./UseEndpoint";

const dummyArrayReturn = [new MyApiItem("test1"), new MyApiItem("test2")];
const handlers = [
    /* Returns a list of two fake api items */
    rest.get(
        "https://test.prime.cdc.gov/api/test/test/:id",
        (_req, res, ctx) => {
            return res(ctx.status(200), ctx.json(new MyApiItem("test")));
        }
    ),
    rest.get("https://test.prime.cdc.gov/api/test/test", (_req, res, ctx) => {
        return res(ctx.status(200), ctx.json(dummyArrayReturn));
    }),
];

const mockSession = {
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
};

/* TEST SERVER TO USE IN `.test.ts` FILES */
const testServer = setupServer(...handlers);

describe("useEndpoint", () => {
    beforeAll(() => testServer.listen());
    afterEach(() => testServer.resetHandlers());
    afterAll(() => testServer.close());
    test("Returns single object data", async () => {
        mockSessionContext.mockReturnValue(mockSession);
        const { result, waitForNextUpdate } = renderHook(() =>
            useEndpoint<{ id: number }, MyApiItem>(MyApi, "itemById", "GET", {
                id: 123,
            })
        );
        expect(result.current.loading).toEqual(true);
        await waitForNextUpdate();
        expect(result.current.data).toEqual(new MyApiItem("test"));
    });

    test("Returns array data", async () => {
        mockSessionContext.mockReturnValue(mockSession);
        const { result, waitForNextUpdate } = renderHook(() =>
            useEndpoint<{}, MyApiItem>(MyApi, "list", "GET")
        );
        act(() => result.current.trigger());
        await waitForNextUpdate();
        expect(result.current.data).toEqual(dummyArrayReturn);
    });

    test("passesObjCompare", () => {
        const result = passesObjCompare({ message: "test" }, SimpleError);
        expect(result).toBeTruthy();
        const badResult = passesObjCompare(
            { failureMessage: "test" },
            SimpleError
        );
        expect(badResult).toBeFalsy();
    });
});
