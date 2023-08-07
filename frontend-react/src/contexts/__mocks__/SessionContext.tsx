import * as SessionContextModule from "../SessionContext";

export const mockSessionContext = jest.spyOn(
    SessionContextModule,
    "useSessionContext",
);
