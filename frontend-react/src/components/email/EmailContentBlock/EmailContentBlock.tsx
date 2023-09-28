export interface EmailContentBlockProps extends React.PropsWithChildren {
    type?: "header" | "content" | "footer";
    isCentered?: boolean;
    isFirst?: boolean;
    isInfo?: boolean;
}

export function EmailContentBlock({
    children,
    type = "content",
    isFirst = false,
    isCentered = false,
    isInfo = false,
}: EmailContentBlockProps) {
    let style = {},
        attrs = {};

    switch (type) {
        case "header":
            style = {
                color: "#5e5e5e",
                fontSize: "22px",
                lineHeight: "22px",
            };
            break;
        case "footer":
            style = {
                fontSize: "12px",
                padding: "16px 0 30px 50px",
                color: "#999",
            };
            break;
        case "content":
            style = {
                paddingTop: "24px",
                verticalAlign: isFirst ? "bottom" : undefined,
            };
            break;
    }

    if (isCentered) {
        attrs = {
            ...attrs,
            align: "center",
        };
        style = {
            ...style,
            height: "32px",
            paddingTop: "24px",
        };
    }

    if (isInfo) {
        style = {
            ...style,
            color: "#999",
        };
    }

    return (
        <tr>
            <td style={style} {...attrs}>
                {children}
            </td>
        </tr>
    );
}
