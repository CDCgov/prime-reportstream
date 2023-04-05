import { renderHook, RenderHookResult } from "@testing-library/react";

import { AppWrapper } from "../../../utils/CustomRenderUtils";
import { UseSenderResourceHookResult } from "../../../hooks/UseSenderResource";
import {
    CustomerStatus,
    FileType,
} from "../../../utils/TemporarySettingsAPITypes";
import * as useSenderResourceExports from "../../../hooks/UseSenderResource";

import useSenderSchemaOptions, {
    STANDARD_SCHEMA_OPTIONS,
    UseSenderSchemaOptionsHookResult,
} from "./";

describe("useSenderSchemaOptions", () => {
    let renderer: RenderHookResult<UseSenderSchemaOptionsHookResult, unknown>;

    function doRenderHook({
        senderDetail = undefined,
        senderIsLoading = false,
        isInitialLoading = false,
    }: UseSenderResourceHookResult) {
        jest.spyOn(
            useSenderResourceExports,
            "useSenderResource"
        ).mockReturnValue({
            senderDetail,
            senderIsLoading,
            isInitialLoading,
        });

        return renderHook(() => useSenderSchemaOptions(), {
            wrapper: AppWrapper(),
        });
    }

    describe("when not logged in", () => {
        beforeEach(() => {
            renderer = doRenderHook({
                senderIsLoading: false,
                isInitialLoading: false,
            });
        });

        test("returns the standard schema options", () => {
            expect(renderer.result.current.isLoading).toEqual(false);
            expect(renderer.result.current.schemaOptions).toEqual(
                STANDARD_SCHEMA_OPTIONS
            );
        });
    });

    describe("when logged in", () => {
        describe("when sender detail query is loading", () => {
            beforeEach(() => {
                renderer = doRenderHook({
                    senderIsLoading: true,
                    isInitialLoading: true,
                });
            });

            test("returns the loading state and the standard schema options", () => {
                expect(renderer.result.current.isLoading).toEqual(true);
                expect(renderer.result.current.schemaOptions).toEqual(
                    STANDARD_SCHEMA_OPTIONS
                );
            });
        });

        describe("when sender detail query is disabled", () => {
            beforeEach(() => {
                renderer = doRenderHook({
                    senderIsLoading: true,
                    isInitialLoading: false,
                });
            });

            test("returns the loading state and the standard schema options", () => {
                expect(renderer.result.current.isLoading).toEqual(false);
                expect(renderer.result.current.schemaOptions).toEqual(
                    STANDARD_SCHEMA_OPTIONS
                );
            });
        });

        describe("as a Sender", () => {
            describe("when the Sender has a different schema than the standard schema options", () => {
                beforeEach(() => {
                    renderer = doRenderHook({
                        senderDetail: {
                            allowDuplicates: true,
                            customerStatus: CustomerStatus.ACTIVE,
                            format: FileType.CSV,
                            name: "test",
                            organizationName: "test",
                            processingType: "sync",
                            schemaName: "test/test-csv",
                            topic: "covid-19",
                        },
                        senderIsLoading: true,
                        isInitialLoading: false,
                    });
                });

                test("returns the standard schema options in addition to the Sender schema", () => {
                    expect(renderer.result.current.isLoading).toEqual(false);
                    expect(renderer.result.current.schemaOptions).toEqual([
                        {
                            format: FileType.CSV,
                            title: "test/test-csv (CSV)",
                            value: "test/test-csv",
                        },
                        ...STANDARD_SCHEMA_OPTIONS,
                    ]);
                });
            });

            describe("when the Sender has the same schema as one of the standard schema options", () => {
                beforeEach(() => {
                    const schemaOption = STANDARD_SCHEMA_OPTIONS[0];

                    renderer = doRenderHook({
                        senderDetail: {
                            allowDuplicates: true,
                            customerStatus: CustomerStatus.ACTIVE,
                            format: schemaOption.format,
                            name: "test",
                            organizationName: "test",
                            processingType: "sync",
                            schemaName: schemaOption.value,
                            topic: "covid-19",
                        },
                        senderIsLoading: true,
                        isInitialLoading: false,
                    });
                });

                test("returns the de-duplicated standard schema options", () => {
                    expect(renderer.result.current.isLoading).toEqual(false);
                    expect(renderer.result.current.schemaOptions).toEqual(
                        STANDARD_SCHEMA_OPTIONS
                    );
                });
            });
        });
    });
});
