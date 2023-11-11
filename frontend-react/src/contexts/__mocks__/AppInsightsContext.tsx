import * as AppInsightsContextModule from "../AppInsights";

export const mockAppInsights = {
    trackEvent: jest.fn(),
};

export function mockAppInsightsContextReturnValue(
    impl?: Partial<AppInsightsContextModule.AppInsightsCtx>,
) {
    const mockAppInsightsContext = jest.spyOn(
        AppInsightsContextModule,
        "useAppInsightsContext",
    );
    return mockAppInsightsContext.mockReturnValue({
        fetchHeaders: () => ({}),
        appInsights: mockAppInsights,
        ...impl,
    } as any);
}
