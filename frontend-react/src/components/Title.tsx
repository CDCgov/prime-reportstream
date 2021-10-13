import React from "react";

interface TitleProps {
    /* The string above the bold title */
    preTitle?: string;
    /* The bold title */
    title: string;
}

function Title({ preTitle, title }: TitleProps) {
    const preTitleJSX = preTitle ? (
        <h3 className="margin-bottom-0">
            <span className="text-normal text-base margin-bottom-0">
                {preTitle}
            </span>
        </h3>
    ) : undefined;
    return (
        <section className="margin-bottom-5 maxw-mobile-lg">
            {preTitleJSX ? preTitleJSX : null}
            <h1 className="margin-top-0 margin-bottom-0">{title}</h1>
        </section>
    );
}

export default Title;
