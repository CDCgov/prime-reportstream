/* INFO
   this interface is structured to provide typing to sections passed into components as
   props in Home.tsx and its subcomponents. Content that fits this model can be found in
   content/content.json */
export interface SectionProp {
    title?: string;
    type?: string;
    summary?: string;
    bullets?: { content?: string }[];
    features?: FeatureProp[];
    description?: string;
    buttonText?: string;
    buttonUrlSubject?: string;
}

/* INFO
   this interface is structured to provide typing to features passed into components as
   props in Home.tsx and its subcomponents. Content that fits this model can be found in
   content/content.json */
export interface FeatureProp {
    method?: number;
    title?: string;
    icon?: string;
    img?: string;
    imgAlt?: string;
    summary?: string;
    items?: { title?: string; summary?: string }[];
}
