export const mockCtx = {
    toast: jest.fn(),
};

jest.mock<typeof import("../Toast")>("../Toast", () => ({
    ...jest.requireActual("../Toast"),
    useToast: jest.fn().mockImplementation(() => mockCtx),
}));
