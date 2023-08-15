/* INFO
   this interface is structured to provide typing to sections passed into components as
   props in Home.tsx and its subcomponents. */
export interface SectionProp {
    title?: string;
    type?: string;
    summary?: string;
    subTitle?: string;
    bullets?: { content?: string }[];
    items?: ItemProp[];
    description?: string;
    buttonText?: string;
    buttonUrlSubject?: string;
    citation?: CitationProp[];
}

/* INFO
   this interface is structured to provide typing to items passed into components as
   props in Home.tsx and its subcomponents.  */
export interface ItemProp {
    method?: number;
    title?: string;
    icon?: string;
    img?: string;
    imgAlt?: string;
    imgClassName?: string;
    summary?: string;
    items?: { title?: string; summary?: string }[];
}

export interface CitationProp {
    title?: string;
    quote?: string;
    author?: string;
    authorTitle?: string;
}
