import {
    EmailContentBlock,
    EmailContentBlockProps,
} from "../EmailContentBlock/EmailContentBlock";

export interface EmailContainerProps extends React.PropsWithChildren {
    type?: "outer" | "inner" | "centered";
}

export function EmailContainer({
    children,
    type = "centered",
}: EmailContainerProps) {
    let style = {},
        attrs = {
            cellPadding: "0",
        } as Record<string, string | number>;
    switch (type) {
        case "outer":
            attrs = {
                ...attrs,
                border: 0,
                cellSpacing: "0",
                valign: "top",
                align: "left",
            };
            style = {
                fontFamily:
                    '"Proxima Nova", "Century Gothic", Arial, Verdana, sans-serif',
                fontSize: "14px",
                color: "#5e5e5e",
                width: "98%",
                maxWidth: "600px",
                float: "none",
                margin: "0 auto",
            };
            break;
        case "inner":
            attrs = {
                bgcolor: "#ffffff",
            };
            style = {
                width: "100%",
                lineHeight: "17px",
                padding: "32px",
                border: "1px solid",
                borderColor: "#f0f0f0",
            };
            break;
        case "centered":
            attrs = {
                border: 0,
                cellPadding: "0",
                cellSpacing: "0",
                valign: "top",
            };
            break;
    }
    const table = (
        <table style={style} {...attrs}>
            {children}
        </table>
    );

    if (type === "centered") {
        return (
            <tr>
                <td align="center">{table}</td>
            </tr>
        );
    }

    return table;
}

export function EmailContainerCentered(
    props: Omit<EmailContainerProps, "type">,
) {
    return <EmailContainer {...props} type="centered" />;
}

EmailContainerCentered.ContentBlock =
    function EmailContainerCenteredContentBlock(
        props: Omit<EmailContentBlockProps, "type" | "isCentered">,
    ) {
        return <EmailContentBlock {...props} isCentered type="content" />;
    };

export default EmailContainer;
