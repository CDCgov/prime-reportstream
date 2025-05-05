module.exports = {
    useOktaAuth: vi.fn().mockReturnValue({
        authState: {},
        oktaAuth: {},
    }),
    Security: vi.fn().mockImplementation(({ children }: any) => <>{children}</>),
};
