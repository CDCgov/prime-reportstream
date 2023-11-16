export const mockEvent = (mock?: Partial<any>) => {
    return {
        response: mock?.response || null,
    };
};
