export const mockAppInsights = {
    trackEvent: jest.fn(),
    trackTrace: jest.fn(),
    trackException: jest.fn(),
    customProperties: {},
    properties: {
        context: {
            getSessionId: jest.fn().mockReturnValue("test"),
            client: {},
            user: {},
        },
    },
};
