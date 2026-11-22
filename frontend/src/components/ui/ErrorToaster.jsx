import { useEffect, useRef, useState } from 'react';
import { subscribeToHttpErrors } from '../../api/setupHttp.js';

const VISIBLE_MS = 6000;
const MAX_TOASTS = 3;

// Shows failed requests the same way on every screen: a danger notice at the bottom, gone after a few seconds.
export default function ErrorToaster() {
    const [toasts, setToasts] = useState([]);
    const nextId = useRef(0);

    useEffect(() => {
        const timers = new Set();
        const unsubscribe = subscribeToHttpErrors((message) => {
            const id = nextId.current++;
            setToasts((current) => {
                // The same message twice in a row (a screen firing parallel requests) shows once.
                if (current.some((toast) => toast.message === message)) return current;
                return [...current, { id, message }].slice(-MAX_TOASTS);
            });
            const timer = setTimeout(() => {
                timers.delete(timer);
                setToasts((current) => current.filter((toast) => toast.id !== id));
            }, VISIBLE_MS);
            timers.add(timer);
        });
        return () => {
            unsubscribe();
            timers.forEach(clearTimeout);
        };
    }, []);

    if (toasts.length === 0) return null;

    return (
        <div
            dir="rtl"
            className="fixed bottom-4 inset-x-4 z-[300] flex flex-col items-center gap-2 pointer-events-none"
            aria-live="assertive"
        >
            {toasts.map((toast) => (
                <div key={toast.id} role="alert" className="glass glass--strong rounded-2xl pointer-events-auto max-w-lg w-full">
                    <button
                        type="button"
                        className="notice notice--danger w-full"
                        onClick={() => setToasts((current) => current.filter((t) => t.id !== toast.id))}
                        aria-label={`${toast.message} — סגירה`}
                    >
                        {toast.message}
                    </button>
                </div>
            ))}
        </div>
    );
}
