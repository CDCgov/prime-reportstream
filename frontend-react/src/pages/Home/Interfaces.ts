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

export interface FeatureProp {
    method?: number;
    title?: string;
    icon?: string;
    img?: string;
    imgAlt?: string;
    summary?: string;
    items?: { title?: string; summary?: string }[];
}