import { describe, expect, it } from 'vitest';
import { initSentry } from './sentry.js';

describe('initSentry', () => {
    it('is a no-op without VITE_SENTRY_DSN', async () => {
        await expect(initSentry({ MODE: 'test' })).resolves.toBe(false);
    });
});
