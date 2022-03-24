import { groupToOrg } from "./OrganizationUtils";

test("groupToOrg", () => {
    const admins = groupToOrg("DHPrimeAdmins");
    const ignoreWaters = groupToOrg("DHSender_ignore.ignore-waters");
    const mdPhd = groupToOrg("DHmd_phd");
    const simpleReport = groupToOrg("simple_report");
    const malformedGroupName = groupToOrg("DHSender_test_org");

    expect(admins).toBe("PrimeAdmins");
    expect(ignoreWaters).toBe("ignore.ignore-waters");
    expect(mdPhd).toBe("md-phd");
    expect(simpleReport).toBe("simple_report");
    expect(malformedGroupName).toBe("test_org");
});
