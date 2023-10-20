import * as AppInsightsContextModule from "../AppInsightsContext";

export const mockAppInsightsContext = jest.spyOn(
    AppInsightsContextModule,
    "useAppInsightsContext",
);

export const mockAppInsights = {
    trackEvent: jest.fn(),
} as any;

export function mockAppInsightsContextReturnValue(
    impl?: Partial<AppInsightsContextModule.AppInsightsCtx>,
) {
    return mockAppInsightsContext.mockReturnValue({
        fetchHeaders: {},
        appInsights: mockAppInsights,
        ...impl,
    } as any);
}
