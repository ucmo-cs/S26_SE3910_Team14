import { useMemo, useState } from 'react';
import type { ActivityItem } from '../../api/adminService';

type ActivityFeedProps = {
  items: ActivityItem[];
  onRefresh: () => void;
  refreshing: boolean;
};

export default function ActivityFeed({ items, onRefresh, refreshing }: ActivityFeedProps) {
  const [query, setQuery] = useState('');
  const [actionFilter, setActionFilter] = useState('ALL');

  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      const normalizedAction = item.actionType.toUpperCase();
      const matchesAction =
        actionFilter === 'ALL' ||
        normalizedAction === actionFilter ||
        normalizedAction.includes(actionFilter);
      const normalizedQuery = query.trim().toLowerCase();
      const matchesQuery =
        !normalizedQuery ||
        item.user.toLowerCase().includes(normalizedQuery) ||
        item.details.toLowerCase().includes(normalizedQuery) ||
        item.actionType.toLowerCase().includes(normalizedQuery);
      return matchesAction && matchesQuery;
    });
  }, [items, query, actionFilter]);

  const actionOptions = useMemo(() => {
    const values = new Set<string>();
    items.forEach((item) => values.add(item.actionType.toUpperCase()));
    return Array.from(values).sort();
  }, [items]);

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold text-blue-900">System Activity Viewer</h2>
          <p className="text-sm text-slate-600">
            Database-backed audit events with actor identity and request metadata.
          </p>
        </div>
        <button
          type="button"
          onClick={onRefresh}
          className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
        >
          {refreshing ? 'Refreshing...' : 'Refresh Feed'}
        </button>
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-3">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search user or details"
          className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
        />
        <select
          value={actionFilter}
          onChange={(event) => setActionFilter(event.target.value)}
          className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
        >
          <option value="ALL">All actions</option>
          {actionOptions.map((option) => (
            <option key={option} value={option}>
              {option.replaceAll('_', ' ')}
            </option>
          ))}
        </select>
      </div>

      <div className="mt-4 overflow-x-auto">
        <table className="w-full min-w-[680px] border-collapse text-left text-sm">
          <thead>
            <tr className="border-b border-slate-200 text-slate-600">
              <th className="py-2 pr-4 font-medium">Timestamp</th>
              <th className="py-2 pr-4 font-medium">Action</th>
              <th className="py-2 pr-4 font-medium">User</th>
              <th className="py-2 font-medium">Details</th>
            </tr>
          </thead>
          <tbody>
            {filteredItems.length === 0 ? (
              <tr>
                <td className="py-4 text-slate-500" colSpan={4}>
                  No activity matches your filters.
                </td>
              </tr>
            ) : (
              filteredItems.map((item) => (
                <tr key={item.id} className="border-b border-slate-100">
                  <td className="py-2 pr-4 text-slate-600">
                    {new Date(item.timestamp).toLocaleString()}
                  </td>
                  <td className="py-2 pr-4 font-medium text-blue-900">{item.actionType}</td>
                  <td className="py-2 pr-4 text-slate-700">{item.user}</td>
                  <td className="py-2 text-slate-700">
                    <span className="break-words whitespace-pre-wrap">{item.details}</span>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
