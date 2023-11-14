export const mockCtx = {
    toast: vi.fn(),
};

const ToastModule = await vi.importActual<typeof import("../")>("../");

module.exports = {
    ...ToastModule,
    useToast: vi.fn(() => mockCtx),
};
