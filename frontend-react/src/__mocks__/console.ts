/* eslint-disable no-console */
function mockResetAll() {
    jest.mocked(console.assert).mockReset();
    jest.mocked(console.debug).mockReset();
    jest.mocked(console.error).mockReset();
    jest.mocked(console.info).mockReset();
    jest.mocked(console.log).mockReset();
    jest.mocked(console.trace).mockReset();
    jest.mocked(console.warn).mockReset();
}
function mockRestoreAll() {
    jest.mocked(console.assert).mockRestore();
    jest.mocked(console.debug).mockRestore();
    jest.mocked(console.error).mockRestore();
    jest.mocked(console.info).mockRestore();
    jest.mocked(console.log).mockRestore();
    jest.mocked(console.trace).mockRestore();
    jest.mocked(console.warn).mockRestore();
}
export const mockConsole = {
    warn: jest.spyOn(console, "warn"),
    error: jest.spyOn(console, "error"),
    log: jest.spyOn(console, "log"),
    assert: jest.spyOn(console, "assert"),
    debug: jest.spyOn(console, "debug"),
    info: jest.spyOn(console, "info"),
    trace: jest.spyOn(console, "trace"),
    mockResetAll,
    mockRestoreAll,
};
