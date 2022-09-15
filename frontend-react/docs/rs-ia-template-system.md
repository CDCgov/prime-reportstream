# IA Template System

To make our public site content easier to update, and to separate engineering, design, and content
concerns, we created a templating system for public site pages.

## How it works

All the components can be found in `src/components/Content`, and they run on a single type
of object, the `ContentDirectory`. 

To use a pre-existing template, simple create a page, import the required variables from
the content directory, and render. Here's an example from our `Product` page.

```typescript jsx
const rootProps: IATemplateProps<IASideNavProps> = {
    pageName: ProductDirectoryTools.title,
    subtitle: ProductDirectoryTools.subtitle,
    templateKey: TemplateName.SIDE_NAV, 
    templateProps: {
        directories: productDirectories,
    },
};
export const Product = () => <IATemplate {...rootProps} />;
```

Using the `IATemplateProps<T>` interface will help you accurately type the props object.
Once created, pass the props in by destructuring them on the `IATemplate` component in your
return statement.

### To route, or not to route?

The `SIDE_NAV` template is an example of a template with a built-in router. If your template
requires you build a router into it, as was the case when coupling the side-nav with 
markdown pages, you can define the props like above.

If, however, you would like the template system to generate routing for you (i.e. your components
don't handle their own routing), as is the case in `CARD_GRID`, then you can include an optional
prop: `includeRouter: true`. Setting this to true will generate a router from your directories.

## Make a template

To create a template, begin with a main entry point; a component that takes a `ContentDirectory[]`
and returns a generated page.

```typescript jsx
export interface IASampleTemplateProps {
    directories: ContentDirectory[];
}

const IASampleTemplate = ({ directories }: IASampleTemplateProps) => {
    return <></>;
};

export default IASampleTemplate;
```

To keep things as reusable as possible, try to create a component tree that doesn't require
any side effects provided by the template itself. 

- Using value from `ContentDirectory` to construct an object you pass to a subcomponent ❌
- Passing in the value from `ContentDirectory` and letting the subcomponent construct the object ✅

### Activate template

To activate a template, first add it to the `TemplateName` enum in `IATemplate.tsx`. 

```typescript
/** Template names! Add the universal template key here whenever
 * you make a new template. */
export enum TemplateName {
    CARD_GRID = "card-grid",
    SIDE_NAV = "side-nav",
    NEW_TEMP = "new-template", // <-- It's new!
}
```
Then add the condition to the `template` callback at the top of the `IATemplate` component.
```typescript jsx
const template = useCallback((key: TemplateName) => {
    switch (key) {
        case TemplateName.CARD_GRID:
            return <IACardGridTemplate {...templateProps} />;
        case TemplateName.SIDE_NAV:
            return <IASideNavTemplate {...templateProps} />;
        case TemplateName.NEW_TEMP:
            return <IANewTemplate {...templateProps} />; // <-- It's new!
    }
}, []); // eslint-disable-line
```
