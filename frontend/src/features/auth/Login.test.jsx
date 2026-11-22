import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import Login from './Login.jsx';

const renderLogin = () => render(<MemoryRouter><Login /></MemoryRouter>);

describe('Login', () => {
    it('keeps the submit button disabled until both fields are filled', () => {
        renderLogin();
        const submit = screen.getByRole('button', { name: 'התחבר' });
        expect(submit).toBeDisabled();

        fireEvent.change(screen.getByPlaceholderText('תעודת זהות'), { target: { value: '123456789' } });
        expect(submit).toBeDisabled();

        fireEvent.change(document.querySelector('input[name="password"]'), { target: { value: 'secret' } });
        expect(submit).toBeEnabled();
    });
});
