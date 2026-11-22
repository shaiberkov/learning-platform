import { useTranslation } from "react-i18next";

// Skeleton placeholders shown while data loads, instead of a spinner. Screen readers
// hear one polite "loading" announcement; the grey blocks themselves are hidden from them.

export function Skeleton({ className = "", style }) {
    return <span aria-hidden="true" className={`skeleton block ${className}`} style={style} />;
}

export function SkeletonList({ rows = 4, className = "" }) {
    const { t } = useTranslation();
    return (
        <div role="status" aria-live="polite" className={`space-y-3 ${className}`}>
            <span className="sr-only">{t("common.loading")}</span>
            {Array.from({ length: rows }, (_, i) => (
                <div key={i} className="row-card space-y-2" aria-hidden="true">
                    <Skeleton className="h-3 w-1/3" />
                    <Skeleton className="h-4 w-3/4" />
                    <Skeleton className="h-3 w-1/2" />
                </div>
            ))}
        </div>
    );
}

// Fallback for lazily loaded pages: the rough shape of a dashboard.
export function PageSkeleton() {
    const { t } = useTranslation();
    return (
        <div role="status" aria-live="polite" className="page page--wide space-y-5">
            <span className="sr-only">{t("common.loading")}</span>
            <div className="glass p-6 sm:p-8 space-y-4" aria-hidden="true">
                <Skeleton className="h-3 w-32" />
                <Skeleton className="h-9 w-2/3 max-w-xl" />
                <Skeleton className="h-4 w-1/2 max-w-md" />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4" aria-hidden="true">
                {[0, 1, 2].map((i) => (
                    <div key={i} className="glass p-5 space-y-3">
                        <Skeleton className="h-10 w-10 !rounded-xl" />
                        <Skeleton className="h-4 w-2/3" />
                        <Skeleton className="h-3 w-1/2" />
                    </div>
                ))}
            </div>
        </div>
    );
}
