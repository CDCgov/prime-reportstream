import { renderHook } from "@testing-library/react-hooks";

import { lookupTableServer } from "../__mocks__/LookupTableMockServer";
import { LookupTables, ValueSet } from "../network/api/LookupTableApi";

import useLookupTable, {
    generateUseLookupTable,
    GetLatestData,
    GetLatestVersion,
    getSenderAutomationData,
} from "./UseLookupTable";

jest.mock("@okta/okta-react", () => ({
    useOktaAuth: () => {
        const authState = {
            isAuthenticated: true,
        };
        return { authState: authState };
    },
}));

describe("test all hooks and methods", () => {
    beforeAll(() => lookupTableServer.listen());
    afterEach(() => lookupTableServer.resetHandlers());
    afterAll(() => lookupTableServer.close());

    test("test GetLatestVersion", async () => {
        let version = await GetLatestVersion(LookupTables.VALUE_SET);
        expect(version).toEqual(2);
    });

    test("test GetLatestData", async () => {
        let data = (await GetLatestData<ValueSet>(
            2,
            LookupTables.VALUE_SET
        )) as ValueSet[];
        expect(data[0].name).toEqual(LookupTables.VALUE_SET);
    });

    test("test getSenderAutomationData", async () => {
        let data = await getSenderAutomationData<ValueSet>(
            LookupTables.VALUE_SET
        );
        expect(data.length).toEqual(3);
    });

    test("test useLookupTable hook", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useLookupTable<ValueSet>(LookupTables.VALUE_SET)
        );
        await waitForNextUpdate();
        expect(result.current.length).toEqual(3);
    });

    test("test generateUseLookupTable hook", async () => {
        const generatedHook = generateUseLookupTable<ValueSet>(
            LookupTables.VALUE_SET
        );
        const { result, waitForNextUpdate } = renderHook(() => generatedHook());
        await waitForNextUpdate();
        expect(result.current.length).toEqual(3);
    });
});
