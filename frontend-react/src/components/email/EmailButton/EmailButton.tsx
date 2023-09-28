import { oktaProperty } from "../utils";

export interface EmailButtonProps
    extends React.AnchorHTMLAttributes<HTMLAnchorElement> {}

export function EmailButton({ children, ...props }: EmailButtonProps) {
    return (
        <a
            style={{
                textDecoration: "none",
            }}
            {...props}
        >
            <span
                style={{
                    padding: "9px 32px 7px 31px",
                    border: "1px solid",
                    textAlign: "center",
                    cursor: "pointer",
                    color: "#ffffff",
                    borderRadius: "3px",
                    backgroundColor: oktaProperty("brand.theme.primaryColor"),
                    borderColor: oktaProperty("brand.theme.primaryColor"),
                    boxShadow: `0 1px 0 ${oktaProperty(
                        "brand.theme.primaryColor",
                    )}`,
                }}
            >
                {children}
            </span>
        </a>
    );
}

export default EmailButton;
