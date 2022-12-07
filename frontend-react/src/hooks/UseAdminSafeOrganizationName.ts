export enum Organizations {
    PRIMEADMINS = "PrimeAdmins",
    IGNORE = "ignore",
}

/** Helper hook that converts `PrimeAdmins` to `ignore`
 * @todo Ticket to make PrimeAdmins an RS org {@link https://github.com/CDCgov/prime-reportstream/issues/4140 #4140}
 * @param orgName {string|undefined} Active membership `parsedName` */
export const useAdminSafeOrganizationName = (orgName: string | undefined) => {
    return orgName === Organizations.PRIMEADMINS
        ? Organizations.IGNORE
        : orgName;
};
