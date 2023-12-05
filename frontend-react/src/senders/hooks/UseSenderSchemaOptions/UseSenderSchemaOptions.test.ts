import { renderHook } from "../../../utils/CustomRenderUtils";
import { FileType } from "../../../utils/TemporarySettingsAPITypes";
import * as useSenderResourceExports from "../../../hooks/UseSenderResource";
import { UseSenderResourceHookResult } from "../../../hooks/UseSenderResource";
import { RSSender } from "../../../config/endpoints/settings";
import { dummySender } from "../../../__mocks__/OrganizationMockServer";

import useSenderSchemaOptions, {
    STANDARD_SCHEMA_OPTIONS,
    StandardSchema,
} from "./";

describe("useSenderSchemaOptions", () => {
    const DEFAULT_SENDER: RSSender = {
        name: "testSender",
        organizationName: "testOrg",
        format: "CSV",
        topic: "covid-19",
        customerStatus: "testing",
        schemaName: StandardSchema.CSV,
        allowDuplicates: false,
        processingType: "sync",
        version: 0,
    };

    function doRenderHook({ data = DEFAULT_SENDER, isLoading = false }) {
        jest.spyOn(useSenderResourceExports, "default").mockReturnValue({
            data,
            isLoading,
        } as UseSenderResourceHookResult);

        return renderHook(() => useSenderSchemaOptions());
    }

    describe("when not logged in", () => {
        function setup() {
            return doRenderHook({
                isLoading: false,
            });
        }

        test("returns the standard schema options", () => {
            const { result } = setup();
            expect(result.current.isLoading).toEqual(false);
            expect(result.current.data).toEqual(STANDARD_SCHEMA_OPTIONS);
        });
    });

    describe("when logged in", () => {
        describe("when sender detail query is loading", () => {
            function setup() {
                return doRenderHook({
                    isLoading: true,
                });
            }

            test("returns the loading state and the standard schema options", () => {
                const { result } = setup();
                expect(result.current.isLoading).toEqual(true);
                expect(result.current.data).toEqual(STANDARD_SCHEMA_OPTIONS);
            });
        });

        describe("when sender detail query is disabled", () => {
            function setup() {
                return doRenderHook({
                    isLoading: false,
                });
            }

            test("returns the loading state and the standard schema options", () => {
                const { result } = setup();
                expect(result.current.isLoading).toEqual(false);
                expect(result.current.data).toEqual(STANDARD_SCHEMA_OPTIONS);
            });
        });

        describe("as a Sender", () => {
            describe("when the Sender has a different schema than the standard schema options", () => {
                function setup() {
                    return doRenderHook({
                        data: dummySender,
                        isLoading: false,
                    });
                }

                test("returns the standard schema options in addition to the Sender schema", () => {
                    const { result } = setup();
                    expect(result.current.isLoading).toEqual(false);
                    expect(result.current.data).toEqual([
                        {
                            format: FileType.CSV,
                            title: "test/covid-19-test (CSV)",
                            value: "test/covid-19-test",
                        },
                        ...STANDARD_SCHEMA_OPTIONS,
                    ]);
                });
            });

            describe("when the Sender has the same schema as one of the standard schema options", () => {
                function setup() {
                    return doRenderHook({
                        isLoading: false,
                    });
                }

                test("returns the de-duplicated standard schema options", () => {
                    const { result } = setup();
                    expect(result.current.isLoading).toEqual(false);
                    expect(result.current.data).toEqual(
                        STANDARD_SCHEMA_OPTIONS,
                    );
                });
            });
        });
    });
});
