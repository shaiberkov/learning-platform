// Service worker, built by vite-plugin-pwa (injectManifest).
// - Precaches the app shell and every built chunk, so the app opens offline.
// - Serves index.html for in-app navigations (React Router handles the path).
// - Caches Google Fonts. API calls are never cached here: they carry the user's token,
//   and offline practice keeps its own data in IndexedDB (src/offline/practiceStore.js).
// - Shows Web Push notifications and opens their link on click.
import { cleanupOutdatedCaches, createHandlerBoundToURL, precacheAndRoute } from "workbox-precaching";
import { NavigationRoute, registerRoute } from "workbox-routing";
import { CacheFirst, StaleWhileRevalidate } from "workbox-strategies";
import { clientsClaim } from "workbox-core";

self.skipWaiting();
clientsClaim();

precacheAndRoute(self.__WB_MANIFEST);
cleanupOutdatedCaches();

registerRoute(new NavigationRoute(createHandlerBoundToURL("index.html")));

registerRoute(
    ({ url }) => url.origin === "https://fonts.googleapis.com",
    new StaleWhileRevalidate({ cacheName: "google-fonts-stylesheets" })
);
registerRoute(
    ({ url }) => url.origin === "https://fonts.gstatic.com",
    new CacheFirst({ cacheName: "google-fonts-webfonts" })
);

self.addEventListener("push", (event) => {
    let data = {};
    try {
        data = event.data ? event.data.json() : {};
    } catch {
        data = { title: event.data ? event.data.text() : "" };
    }
    const title = data.title || "מרחב הלמידה";
    event.waitUntil(
        self.registration.showNotification(title, {
            body: data.body || "",
            icon: "/pwa-192x192.png",
            badge: "/pwa-192x192.png",
            dir: "auto",
            lang: "he",
            tag: data.id ? `notification-${data.id}` : undefined,
            data: { link: data.link || "/" },
        })
    );
});

self.addEventListener("notificationclick", (event) => {
    event.notification.close();
    const target = new URL(event.notification.data?.link || "/", self.location.origin).href;
    event.waitUntil(
        self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((windows) => {
            const open = windows.find((w) => w.url.startsWith(self.location.origin));
            if (open) {
                open.focus();
                return open.navigate(target);
            }
            return self.clients.openWindow(target);
        })
    );
});
