import { MapPin } from 'lucide-react';
import { useEffect, useState } from 'react';
import { getBranches } from '../../api/bookingService';
import { useBooking, type Branch } from '../../context/BookingContext';

export default function Step2Locations() {
  const [branches, setBranches] = useState<Branch[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { selectedTopic, selectedBranch, setSelectedBranch, goToNextStep, goToPreviousStep } =
    useBooking();

  const loadBranches = (topicId: number) => {
    setLoading(true);
    setError('');
    getBranches(topicId)
      .then((items) => setBranches(items))
      .catch(() => setError('Unable to load branches for this topic. Please try again.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (!selectedTopic) {
      setBranches([]);
      setLoading(false);
      return;
    }

    loadBranches(selectedTopic.id);
  }, [selectedTopic]);

  const handleChooseBranch = (branch: Branch) => {
    setSelectedBranch(branch);
    goToNextStep();
  };

  const formatAddress = (branch: Branch) =>
    [branch.streetLine1, branch.streetLine2, [branch.city, branch.stateProvince].filter(Boolean).join(', '), branch.postalCode]
      .filter(Boolean)
      .join(' ');

  if (!selectedTopic) {
    return (
      <section className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-700">
        Please select a topic first.
      </section>
    );
  }

  return (
    <section className="space-y-6 rounded-2xl border border-slate-200 bg-white p-6 md:p-8">
      <header className="space-y-2">
        <h2 className="text-2xl font-semibold text-blue-900">Select a Branch Location</h2>
        <p className="text-sm text-slate-600">
          Showing locations available for {selectedTopic.name}.
        </p>
      </header>

      {loading ? (
        <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Loading branch locations...
        </div>
      ) : error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 p-6 text-sm text-red-700">
          <p>{error}</p>
          <button
            type="button"
            onClick={() => loadBranches(selectedTopic.id)}
            className="mt-3 rounded-md bg-blue-900 px-3 py-2 text-xs font-medium text-white transition hover:bg-blue-800"
          >
            Retry
          </button>
        </div>
      ) : branches.length === 0 ? (
        <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          No branches currently support this topic.
        </div>
      ) : (
        <div className="space-y-3">
          {branches.map((branch) => {
            const isSelected = selectedBranch?.id === branch.id;

            return (
              <button
                type="button"
                key={branch.id}
                onClick={() => handleChooseBranch(branch)}
                className={`w-full rounded-xl border bg-white p-4 text-left transition ${
                  isSelected
                    ? 'border-blue-900 ring-2 ring-slate-300'
                    : 'border-slate-200 hover:border-blue-900'
                }`}
              >
                <div className="flex items-start gap-3">
                  <MapPin className="mt-0.5 h-5 w-5 text-blue-900" />
                  <div>
                    <h3 className="font-semibold text-blue-900">{branch.displayName}</h3>
                    <p className="text-sm text-slate-600">{formatAddress(branch)}</p>
                    <p className="mt-1 text-xs text-slate-500">Timezone: {branch.timeZone}</p>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      )}

      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={goToPreviousStep}
          className="rounded-lg border border-slate-200 bg-white px-5 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
        >
          Back
        </button>
      </div>
    </section>
  );
}
