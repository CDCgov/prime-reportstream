export function InPageNavHeader(props: any) {
    if (typeof props.children !== "string") {
        return props.children;
    }
    return <h2 className="usa-in-page-nav__heading">{props.children}</h2>;
}

export default InPageNavHeader;
