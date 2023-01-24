import { RSSessionContext } from "../../../contexts/SessionContext";
import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import * as UseOktaMemberships from "../../UseOktaMemberships";

const getMembershipsFromToken = jest.spyOn(
    UseOktaMemberships,
    "getMembershipsFromToken"
);

export function mockAuthReturnValue(value: RSSessionContext) {
    mockSessionContext.mockReturnValue(value);
    getMembershipsFromToken.mockReturnValue({
        activeMembership: value.activeMembership,
    });
}
