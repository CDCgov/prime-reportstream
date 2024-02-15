module.exports = {
    useOktaAuth: jest.fn().mockReturnValue({
        authState: {},
        oktaAuth: {},
    }),
    Security: jest
        .fn()
        .mockImplementation(({ children }: any) => <>{children}</>),
};
