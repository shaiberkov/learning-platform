// Optional error reporting. Without VITE_SENTRY_DSN this does nothing and the Sentry SDK is never
// downloaded (it is a separate chunk loaded only when a DSN is configured at build time).
export function initSentry(env = import.meta.env) {
    const dsn = env.VITE_SENTRY_DSN;
    if (!dsn) return Promise.resolve(false);

    return import('@sentry/react')
        .then((Sentry) => {
            Sentry.init({
                dsn,
                environment: env.VITE_SENTRY_ENVIRONMENT || env.MODE,
                release: env.VITE_APP_VERSION || undefined,
                sendDefaultPii: false,
                tracesSampleRate: Number(env.VITE_SENTRY_TRACES_SAMPLE_RATE || 0),
            });
            return true;
        })
        .catch((error) => {
            console.warn('Sentry could not be loaded', error);
            return false;
        });
}
