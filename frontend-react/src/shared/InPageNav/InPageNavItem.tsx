import classNames from "classnames";

export function InPageNavListItem(props: any) {
    const classnames = classNames(
        "usa-in-page-nav__item",
        props.isSubItem ? "usa-in-page-nav__sub-item" : undefined
    );
    return <li className={classnames}>{props.children}</li>;
}

export default InPageNavListItem;
