import useAdminSafeOrganizationName, {
    Organizations,
} from "./UseAdminSafeOrganizationName";
import { renderHook } from "../../utils/CustomRenderUtils";

describe("useAdminSafeOrganizationName", () => {
    test("returns correct client organization", () => {
        const { result } = renderHook(() =>
            useAdminSafeOrganizationName("testOrg"),
        );
        expect(result.current).toEqual("testOrg");
    });

    test("returns correct client organization for prime admins", () => {
        const { result } = renderHook(() =>
            useAdminSafeOrganizationName(Organizations.PRIMEADMINS),
        );
        expect(result.current).toEqual(Organizations.IGNORE);
    });
});
