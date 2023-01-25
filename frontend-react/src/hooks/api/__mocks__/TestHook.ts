import { UseQueryResult } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react-hooks";

import { QueryWrapper } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../UseOktaMemberships";
import { mockAuthReturnValue } from "../__mocks__/OktaAuth";

interface Auth {
    oktaToken: any;
    activeMembership: any;
    dispatch: any;
    initialized: boolean;
}

interface TestScenario<T extends (...args: any[]) => UseQueryResult<any>> {
    name?: string;
    args: Parameters<T>;
    auth: Auth | boolean;
}

interface QueryTestScenarioData<
    T extends (...args: any[]) => UseQueryResult<any>
> extends TestScenario<T> {
    data: T extends (...args: any[]) => UseQueryResult<infer D> ? D : never;
}

interface QueryTestScenarioError<
    T extends (...args: any[]) => UseQueryResult<any>
> extends TestScenario<T> {
    error: T extends (...args: any[]) => UseQueryResult<any, infer E>
        ? E
        : never;
}

const mockAuth = {
    oktaToken: {
        accessToken: "TOKEN",
    },
    activeMembership: {
        memberType: MemberType.RECEIVER,
        parsedName: "testOrg",
        service: "testReceiver",
    },
    dispatch: () => {},
    initialized: true,
};

export function testQueryHook<
    T extends (...args: any[]) => UseQueryResult<any>
>(
    hook: T,
    expectedData: QueryTestScenarioData<T>[],
    expectedErrors?: QueryTestScenarioError<T>[]
) {
    if (expectedData.length) {
        for (let scenario of expectedData) {
            const { name, args, auth, data } = scenario;
            test(name ?? "returned correct data", async () => {
                const _auth = !!auth
                    ? auth === true
                        ? mockAuth
                        : undefined
                    : undefined;
                if (_auth) mockAuthReturnValue(_auth);
                const { result, waitFor } = renderHook(() => hook(...args), {
                    wrapper: QueryWrapper(),
                });
                await waitFor(() => !!result.current.data);
                expect(result.current.error).toBeNull();
                expect(result.current.data).toEqual(data);
                expect(result.current.isLoading).toEqual(false);
            });
        }

        if (expectedErrors?.length) {
            for (let scenario of expectedErrors) {
                const { name, args, auth, error } = scenario;
                test(name ?? "returned correct error", async () => {
                    const _auth = !!auth
                        ? auth === true
                            ? mockAuth
                            : undefined
                        : undefined;
                    if (_auth) mockAuthReturnValue(_auth);
                    const { result, waitFor } = renderHook(() => hook(args), {
                        wrapper: QueryWrapper(),
                    });
                    await waitFor(() => !!result.current.error);
                    expect(result.current.error).toEqual(error);
                    expect(result.current.isLoading).toEqual(false);
                });
            }
        }
    }
}
