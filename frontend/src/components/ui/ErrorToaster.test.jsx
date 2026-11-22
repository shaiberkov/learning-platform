import { act, fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import ErrorToaster from './ErrorToaster.jsx';
import { notifyHttpError } from '../../api/setupHttp.js';

describe('ErrorToaster', () => {
    it('renders nothing until an error is announced', () => {
        const { container } = render(<ErrorToaster />);
        expect(container).toBeEmptyDOMElement();
    });

    it('shows an announced error once and closes it on click', () => {
        render(<ErrorToaster />);

        act(() => {
            notifyHttpError('אין חיבור לשרת');
            notifyHttpError('אין חיבור לשרת');
        });

        const alerts = screen.getAllByRole('alert');
        expect(alerts).toHaveLength(1);
        expect(alerts[0]).toHaveTextContent('אין חיבור לשרת');

        fireEvent.click(screen.getByRole('button', { name: /אין חיבור לשרת/ }));
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
});
