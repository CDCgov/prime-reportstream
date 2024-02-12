module.exports = {
    ...jest.requireActual("../index"),
    useToast: jest.fn().mockReturnValue({
        toast: jest.fn(),
    }),
};
