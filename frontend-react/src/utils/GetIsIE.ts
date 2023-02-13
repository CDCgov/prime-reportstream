const IE_REGEX = /Trident\/|MSIE/;

export function getIsIE(userAgent: string = window.navigator.userAgent) {
    // IE 10 and IE 11
    return IE_REGEX.test(userAgent);
}

export const IS_IE = getIsIE();
