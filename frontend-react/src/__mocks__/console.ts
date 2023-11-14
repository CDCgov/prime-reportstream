/* eslint-disable no-console */
export const mockConsole = {
    warn: vi.spyOn(console, "warn"),
    error: vi.spyOn(console, "error"),
    log: vi.spyOn(console, "log"),
    assert: vi.spyOn(console, "assert"),
    debug: vi.spyOn(console, "debug"),
    info: vi.spyOn(console, "info"),
    trace: vi.spyOn(console, "trace"),
    mockResetAll() {
        vi.mocked(console.assert).mockReset();
        vi.mocked(console.debug).mockReset();
        vi.mocked(console.error).mockReset();
        vi.mocked(console.info).mockReset();
        vi.mocked(console.log).mockReset();
        vi.mocked(console.trace).mockReset();
        vi.mocked(console.warn).mockReset();
    },
    mockRestoreAll() {
        vi.mocked(console.assert).mockRestore();
        vi.mocked(console.debug).mockRestore();
        vi.mocked(console.error).mockRestore();
        vi.mocked(console.info).mockRestore();
        vi.mocked(console.log).mockRestore();
        vi.mocked(console.trace).mockRestore();
        vi.mocked(console.warn).mockRestore();
    },
    mockImplementationAll() {
        this.assert.mockReturnValue(undefined);
        this.debug.mockReturnValue(undefined);
        this.error.mockReturnValue(undefined);
        this.info.mockReturnValue(undefined);
        this.log.mockReturnValue(undefined);
        this.trace.mockReturnValue(undefined);
        this.warn.mockReturnValue(undefined);
    },
};
