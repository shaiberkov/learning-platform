import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';
// Components use react-i18next (Hebrew by default), as main.jsx sets it up for the app.
import '../i18n/index.js';

afterEach(() => {
    cleanup();
});
