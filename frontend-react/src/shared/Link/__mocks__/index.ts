export const mailToLink = "mailto:someone@abc.com";

export const externalUrls = [
    "https://www.google.com",
    "//www.google.com",
    "//google.com",
    mailToLink,
];

export const internalUrls = [
    undefined,
    "",
    "/login",
    "#",
    "login",
    "https://reportstream.cdc.gov/login",
    "https://www.cdc.gov",
    "https://cdc.gov",
    "//reportstream.cdc.gov/login",
    "//www.cdc.gov",
    "//cdc.gov",
];

export const enumProps = {
    size: ["big"],
    accentStyle: ["cool", "warm"],
};

export const enumPropMap = {
    size: "",
    accentStyle: "accent",
};

export const testScenarios = Object.entries(enumProps).map(([key, valueList]) =>
    valueList.map((v) => [key, v, enumPropMap[key as keyof typeof enumProps]]),
);

export const routeUrls = [
    "",
    "/",
    "asdf",
    `//${window.location.host}/asdf`,
    `${window.location.origin}`,
    "#",
    "#asdf",
    "##asdf",
];

export const routeUrlsMap = {
    [`//${window.location.host}/asdf`]: "/asdf",
    [`${window.location.origin}`]: `/`,
};

export const nonRouteUrls = [
    undefined,
    mailToLink,
    "https://www.google.com",
    "http://www.google.com",
    "//www.google.com",
];
