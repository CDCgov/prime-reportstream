import { renderHook } from "@testing-library/react-hooks";
import { rest } from "msw";
import { setupServer } from "msw/node";

import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";
import {
    MembershipController,
    MembershipSettings,
} from "../UseOktaMemberships";
import { SessionController } from "../UseSessionStorage";
import { MyApi, MyApiItem } from "../../network/api/mocks/MockApi";
import { SimpleError } from "../../utils/UsefulTypes";

import useEndpoint, { passesObjCompare } from "./UseEndpoint";

jest.spyOn(global.console, "error");
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
    rest.get(
        "https://test.prime.cdc.gov/api/test/badList",
        (_req, res, ctx) => {
            return res(ctx.status(200), ctx.json({ notTheRightField: "test" }));
        }
    ),
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
        await waitForNextUpdate();
        expect(result.current.data).toEqual(dummyArrayReturn);
    });

    test("Returns undefined data when type check fails", async () => {
        mockSessionContext.mockReturnValue(mockSession);
        // This skips the axios .then() in useRequestConfig for some reason
        // and because of this, we are failing on loading === false
        const { result, waitForNextUpdate } = renderHook(() =>
            useEndpoint<{}, MyApiItem>(MyApi, "badList", "GET")
        );
        await waitForNextUpdate();
        expect(result.current.data).toEqual(undefined);
        expect(result.current.loading).toBeFalsy();
        expect(result.current.error).toEqual("");
    });

    test("Returns local errors", async () => {
        mockSessionContext.mockReturnValue(mockSession);
        const { result } = renderHook(() =>
            useEndpoint<{}, MyApiItem>(MyApi, "list", "POST")
        );
        expect(result.current.data).toBeUndefined();
        expect(result.current.loading).toBeFalsy();
        expect(result.current.error).toEqual(
            "Your config threw an error: Method POST cannot be used by list"
        );
    });

    test("passesObjCompare detects matching and mismatched object shapes", () => {
        const result = passesObjCompare({ message: "test" }, SimpleError);
        expect(result).toBeTruthy();
        const badResult = passesObjCompare(
            { failureMessage: "test" },
            SimpleError
        );
        expect(badResult).toBeFalsy();
    });
});
