import {
    Placement,
    useFloating,
    autoUpdate,
    flip,
    shift,
    useHover,
    useFocus,
    useDismiss,
    useRole,
    useInteractions,
    useMergeRefs,
    FloatingPortal,
    offset,
    OffsetOptions,
} from "@floating-ui/react";
import classNames from "classnames";
import React from "react";

export const TOOLTIP_POSITIONS = ["top", "bottom", "right", "left"];

export interface TooltipOptions {
    initialOpen?: boolean;
    placement?: Placement;
    open?: boolean;
    onOpenChange?: (open: boolean) => void;
    offsetBy?: OffsetOptions;
}

export function useTooltip({
    initialOpen = false,
    placement = "top",
    open: controlledOpen,
    onOpenChange,
    offsetBy,
}: TooltipOptions = {}) {
    const [uncontrolledOpen, setUncontrolledOpen] = React.useState(initialOpen);
    const isControlled = controlledOpen != null;
    const open = controlledOpen ?? uncontrolledOpen;
    const setOpen = React.useCallback(
        (isOpen: boolean) => {
            console.log("ding");
            onOpenChange?.(isOpen);
            if (controlledOpen == null) {
                setUncontrolledOpen(isOpen);
            }
        },
        [onOpenChange, controlledOpen]
    );

    const data = useFloating({
        placement,
        open,
        onOpenChange: setOpen,
        whileElementsMounted: autoUpdate,
        middleware: [
            flip({
                fallbackAxisSideDirection: "start",
            }),
            shift({ padding: 5 }),
            offset(offsetBy),
        ],
    });

    const context = data.context;

    const hover = useHover(context, {
        move: false,
        enabled: !isControlled,
    });
    const focus = useFocus(context, {
        enabled: !isControlled,
    });
    const dismiss = useDismiss(context);
    const role = useRole(context, { role: "tooltip" });

    const interactions = useInteractions([hover, focus, dismiss, role]);

    return React.useMemo(
        () => ({
            open,
            setOpen,
            ...interactions,
            ...data,
        }),
        [open, setOpen, interactions, data]
    );
}

export type TooltipContextType = ReturnType<typeof useTooltip> | null;

export const TooltipContext = React.createContext<TooltipContextType>(null);

export const useTooltipContext = () => {
    const context = React.useContext(TooltipContext);

    if (context == null) {
        throw new Error("Tooltip components must be wrapped in <Tooltip />");
    }

    return context;
};

export type TooltipProps = { children: React.ReactNode } & TooltipOptions;

/**
 * Prepares the tooltip context for the provided children components. Allows for either controlled or
 * uncontrolled tooltips via the onOpenChange prop. You must provide TooltipTrigger and TooltipContent
 * wrapped components and only as children within a tooltip context (which this creates).
 * ```
 * <Tooltip>
 *   <TooltipTrigger>Hover me!</TooltipTrigger>
 *   <TooltipContent>Hello!</TooltipContent>
 * </Tooltip>
 * ```
 */
export function Tooltip({ children, ...options }: TooltipProps) {
    // This can accept any props as options, e.g. `placement`,
    // or other positioning options.
    const tooltip = useTooltip(options);
    return (
        <TooltipContext.Provider value={tooltip}>
            {children}
        </TooltipContext.Provider>
    );
}

export type TooltipTriggerPropsBase = React.HTMLProps<HTMLElement> & {
    asChild?: boolean;
};

export type TooltipTriggerProps = Omit<TooltipTriggerPropsBase, "ref"> &
    React.RefAttributes<HTMLElement>;

/**
 * The target element that triggers the tooltip. Defaults to passing children to a
 * button, but can be used directly via asChild prop (the children component must
 * be created via React.fowardRef!).
 */
export const TooltipTrigger = React.forwardRef<
    HTMLElement,
    TooltipTriggerPropsBase
>(function TooltipTrigger({ children, asChild = false, ...props }, propRef) {
    const context = useTooltipContext();
    const childrenRef = (children as any).ref;
    const ref = useMergeRefs([context.refs.setReference, propRef, childrenRef]);

    // `asChild` allows the user to pass any element as the anchor
    if (asChild && React.isValidElement(children)) {
        // Not ideal to have to resort to manipulating the children data,
        // but this prevents a lot of boilerplate otherwise.
        return React.cloneElement(
            children,
            context.getReferenceProps({
                ref,
                ...props,
                ...children.props,
                "data-state": context.open ? "open" : "closed",
            })
        );
    }

    return (
        <button
            ref={ref}
            // The user can style the trigger based on the state
            data-state={context.open ? "open" : "closed"}
            {...context.getReferenceProps(props)}
            className={"usa-button usa-tooltip"}
        >
            {children}
        </button>
    );
});

export type TooltipContentPropsBase = React.HTMLProps<HTMLDivElement>;

export type TooltipContentProps = Omit<TooltipContentPropsBase, "ref"> &
    React.RefAttributes<HTMLDivElement>;

export const TooltipContent = React.forwardRef<
    HTMLDivElement,
    TooltipContentPropsBase
>(function TooltipContent(props, propRef) {
    const context = useTooltipContext();
    const ref = useMergeRefs([context.refs.setFloating, propRef]);
    // Simplify corner positions to main placements.
    const bestPosition = TOOLTIP_POSITIONS.includes(context.placement)
        ? context.placement
        : TOOLTIP_POSITIONS.find((p) => p.includes(`${context.placement}-`)) ??
          "top";

    if (!context.open) return null;

    return (
        <FloatingPortal>
            <div
                ref={ref}
                style={{
                    position: context.strategy,
                    top: context.y ?? 0,
                    left: context.x ?? 0,
                    ...props.style,
                }}
                {...context.getFloatingProps(props)}
                className={classNames(
                    "usa-tooltip__body",
                    "is-set",
                    "is-visible",
                    bestPosition && `usa-tooltip__body--${bestPosition}`
                )}
            />
        </FloatingPortal>
    );
});
