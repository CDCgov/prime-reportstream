module.exports = {
    __esModule: true,
    ...jest.requireActual("@microsoft/applicationinsights-react-js"),
    useAppInsightsContext: jest.fn().mockReturnValue({
        customProperties: {},
        trackEvent: jest.fn(),
        properties: {
            context: {
                getSessionId: jest.fn().mockReturnValue("test"),
                client: {},
                user: {}
            }
        }
    }),
}