export function getStatusPillClass(status: string): string {
  const normalized = status.toUpperCase();
  if (normalized === 'REQUESTED') {
    return 'border-sky-200 bg-sky-100 text-sky-800';
  }
  if (normalized === 'APPROVED') {
    return 'border-emerald-200 bg-emerald-100 text-emerald-800';
  }
  if (normalized === 'COMPLETED') {
    return 'border-emerald-800 bg-emerald-800 text-white';
  }
  if (normalized === 'CANCELLED') {
    return 'border-red-200 bg-red-100 text-red-700';
  }
  return 'border-slate-200 bg-slate-100 text-slate-700';
}

export function isRequested(status: string): boolean {
  return status.toUpperCase() === 'REQUESTED';
}

export function getStatusCardClass(status: string): string {
  const normalized = status.toUpperCase();
  if (normalized === 'REQUESTED') {
    return 'border-sky-200 bg-sky-50';
  }
  if (normalized === 'APPROVED') {
    return 'border-emerald-200 bg-emerald-50';
  }
  if (normalized === 'COMPLETED') {
    return 'border-emerald-900 bg-emerald-900/5';
  }
  if (normalized === 'CANCELLED') {
    return 'border-red-200 bg-red-50';
  }
  return 'border-slate-200 bg-white';
}
