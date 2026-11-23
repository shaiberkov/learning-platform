import { describe, expect, it } from 'vitest';
import { countAnswered, difficultyLabel, formatClock, formatScore, parseTarget, targetStatusInfo } from './examSim.js';

describe('formatClock', () => {
    it('formats minutes and hours', () => {
        expect(formatClock(0)).toBe('0:00');
        expect(formatClock(65)).toBe('1:05');
        expect(formatClock(3600 + 61)).toBe('1:01:01');
        expect(formatClock(-5)).toBe('0:00');
    });
});

describe('countAnswered', () => {
    it('counts non-blank answers over all sections', () => {
        const sections = [{ questions: [{ id: 1 }, { id: 2 }] }, { questions: [{ id: 3 }] }];
        expect(countAnswered(sections, { 1: '5', 2: '  ', 3: 'x=1' })).toEqual({ answered: 2, total: 3 });
        expect(countAnswered(undefined, {})).toEqual({ answered: 0, total: 0 });
    });
});

describe('parseTarget', () => {
    it('accepts 0-100 with a dot or a comma', () => {
        expect(parseTarget('90')).toEqual({ value: 90 });
        expect(parseTarget('87,5')).toEqual({ value: 87.5 });
    });
    it('rejects empty, non-numeric and out-of-range targets', () => {
        expect(parseTarget('').error).toBeTruthy();
        expect(parseTarget('abc').error).toBeTruthy();
        expect(parseTarget('120').error).toBe('היעד חייב להיות בין 0 ל-100');
        expect(parseTarget('-3').error).toBeTruthy();
    });
});

describe('labels', () => {
    it('maps statuses and difficulty', () => {
        expect(targetStatusInfo('IMPOSSIBLE').tone).toBe('danger');
        expect(targetStatusInfo('ALREADY_SECURED').tone).toBe('mint');
        expect(targetStatusInfo('NO_DATA').tone).toBe('neutral');
        expect(difficultyLabel(95)).toBe('מאתגר מאוד');
        expect(difficultyLabel(null)).toBeNull();
        expect(formatScore(84)).toBe('84');
        expect(formatScore(84.25)).toBe('84.3');
        expect(formatScore(null)).toBe('—');
    });
});
