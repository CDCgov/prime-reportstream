import { HTMLAttributes, PropsWithChildren } from "react";

export interface BlockquoteProps
    extends PropsWithChildren<HTMLAttributes<HTMLElement>> {
    author: string;
    source: string;
}

function Blockquote({ author, source, children, ...props }: BlockquoteProps) {
    return (
        <blockquote {...props}>
            {children}
            <footer>
                {author}
                <cite>{source}</cite>
            </footer>
        </blockquote>
    );
}

export default Blockquote;
