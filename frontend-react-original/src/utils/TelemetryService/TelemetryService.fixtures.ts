export const appInsightsFixture = {
    trackEvent: vi.fn(),
    trackTrace: vi.fn(),
    trackException: vi.fn(),
    customProperties: {},
    properties: {
        context: {
            getSessionId: vi.fn().mockReturnValue("test"),
            client: {},
            user: {},
        },
    },
};
