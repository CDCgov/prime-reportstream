export const createTelemetryService = jest.fn().mockReturnValue({
    appInsights: {},
    reactPlugin: {
        customProperties: {},
    },
});
