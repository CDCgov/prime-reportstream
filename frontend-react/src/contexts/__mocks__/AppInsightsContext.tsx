import * as AppInsightsContextModule from "../AppInsightsContext";

export const mockAppInsights = {
    trackEvent: vi.fn(),
};

export function mockAppInsightsContextReturnValue(
    impl?: Partial<AppInsightsContextModule.AppInsightsCtx>,
) {
    const mockAppInsightsContext = vi.spyOn(
        AppInsightsContextModule,
        "useAppInsightsContext",
    );
    return mockAppInsightsContext.mockReturnValue({
        fetchHeaders: () => ({}),
        appInsights: mockAppInsights,
        ...impl,
    } as any);
}
