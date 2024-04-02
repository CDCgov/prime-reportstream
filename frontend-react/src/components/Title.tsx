interface TitleProps {
    /* The string above the bold title */
    preTitle?: string;
    /* The bold title */
    title: string;
    postTitle?: string;
    removeBottomMargin?: boolean;
}

function Title({ preTitle, title, postTitle, removeBottomMargin }: TitleProps) {
    return (
        <section
            className={`${removeBottomMargin ? "" : "margin-bottom-5"} maxw-mobile-lg`}
        >
            {preTitle && (
                <h2 className="margin-bottom-0">
                    <span className="text-normal font-body-md text-base margin-bottom-0">
                        {preTitle}
                    </span>
                </h2>
            )}
            <h1 className="margin-top-1 margin-bottom-0">{title}</h1>
            {postTitle && (
                <h2 className="margin-bottom-0">
                    <span className="text-normal font-body-md text-base margin-bottom-0">
                        {postTitle}
                    </span>
                </h2>
            )}
        </section>
    );
}

export default Title;
