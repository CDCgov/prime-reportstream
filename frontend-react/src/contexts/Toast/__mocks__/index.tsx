export const ToastContext = {};

export function ToastProvider(props: any) {
    return <>{props.children}</>;
}

export const showToast = vi.fn();

export const useToast = vi.fn().mockReturnValue({
    toast: vi.fn(),
});
