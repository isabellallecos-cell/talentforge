import { describe, expect, it } from 'vitest';

import { availableActions, formatSalary, historyLabel, parseStatusFilter } from './requisition.models';

describe('availableActions', () => {
  it('orders buttons consistently and drops update and unknown actions', () => {
    expect(availableActions(['reject', 'update', 'approve', 'publish']).map((a) => a.action)).toEqual([
      'approve',
      'reject',
    ]);
  });

  it('asks for a note on decisions and requires one to reject', () => {
    const [approve, reject] = availableActions(['approve', 'reject']);
    expect([approve.asksForNote, approve.noteRequired]).toEqual([true, false]);
    expect([reject.asksForNote, reject.noteRequired]).toEqual([true, true]);
    expect(availableActions(['submit'])[0].asksForNote).toBe(false);
  });

  it('shows nothing when the caller can do nothing', () => {
    expect(availableActions([])).toEqual([]);
  });
});

describe('formatSalary', () => {
  it('formats ranges, single bounds and missing salaries', () => {
    expect(formatSalary({ salaryMin: 50000, salaryMax: 70000, salaryCurrency: 'EUR' })).toBe('EUR 50,000–70,000 / year');
    expect(formatSalary({ salaryMin: 60000, salaryMax: 60000, salaryCurrency: 'USD' })).toBe('USD 60,000 / year');
    expect(formatSalary({ salaryMin: 50000, salaryMax: null, salaryCurrency: 'EUR' })).toBe('from EUR 50,000 / year');
    expect(formatSalary({ salaryMin: null, salaryMax: 90000, salaryCurrency: 'GBP' })).toBe('up to GBP 90,000 / year');
    expect(formatSalary({ salaryMin: null, salaryMax: null, salaryCurrency: null })).toBeNull();
  });
});

describe('parseStatusFilter', () => {
  it('accepts known statuses only', () => {
    expect(parseStatusFilter('pending_approval')).toBe('pending_approval');
    expect(parseStatusFilter('archived')).toBeNull();
    expect(parseStatusFilter(undefined)).toBeNull();
  });
});

describe('historyLabel', () => {
  it('labels every recorded action', () => {
    expect(['create', 'update', 'submit', 'withdraw', 'approve', 'reject', 'reopen'].map(historyLabel)).toEqual([
      'Created',
      'Edited',
      'Submitted',
      'Withdrawn',
      'Approved',
      'Rejected',
      'Reopened',
    ]);
  });
});
