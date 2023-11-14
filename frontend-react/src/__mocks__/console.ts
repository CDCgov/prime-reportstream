const warn = vi.spyOn(console, "warn");
const error = vi.spyOn(console, "error");
const log = vi.spyOn(console, "log");
const assert = vi.spyOn(console, "assert");
const debug = vi.spyOn(console, "debug");
const info = vi.spyOn(console, "info");
const trace = vi.spyOn(console, "trace");

/**
 * Replace console with one with spies
 */
vi.stubGlobal("console", {
    ...console,
    warn,
    error,
    log,
    assert,
    debug,
    info,
    trace,
});

export const mockConsole = {
    warn,
    error,
    log,
    assert,
    debug,
    info,
    trace,
    mockResetAll() {
        assert.mockReset();
        debug.mockReset();
        error.mockReset();
        info.mockReset();
        log.mockReset();
        trace.mockReset();
        warn.mockReset();
    },
    mockRestoreAll() {
        assert.mockRestore();
        debug.mockRestore();
        error.mockRestore();
        info.mockRestore();
        log.mockRestore();
        trace.mockRestore();
        warn.mockRestore();
    },
    mockImplementationAll() {
        assert.mockReturnValue(undefined);
        debug.mockReturnValue(undefined);
        error.mockReturnValue(undefined);
        info.mockReturnValue(undefined);
        log.mockReturnValue(undefined);
        trace.mockReturnValue(undefined);
        warn.mockReturnValue(undefined);
    },
};
