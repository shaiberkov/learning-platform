import { QueryClient } from "@tanstack/react-query";

// staleTime 0 keeps the "every screen fetches on mount" rule: cached data is shown at
// once while a fresh request runs, and nothing outlives the tab (no persistence).
export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 0,
            gcTime: 5 * 60 * 1000,
            retry: 1,
            refetchOnWindowFocus: false,
        },
    },
});
