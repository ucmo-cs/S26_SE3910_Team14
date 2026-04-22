import { MapPin } from 'lucide-react';
import { useEffect, useState } from 'react';
import { getBranches } from '../../api/mockService';
import { useBooking, type Branch } from '../../context/BookingContext';

export default function Step2Locations() {
  const [branches, setBranches] = useState<Branch[]>([]);
  const [loading, setLoading] = useState(true);
  const { selectedTopic, selectedBranch, setSelectedBranch, goToNextStep, goToPreviousStep } =
    useBooking();

  useEffect(() => {
    if (!selectedTopic) {
      setBranches([]);
      setLoading(false);
      return;
    }

    setLoading(true);
    getBranches(selectedTopic.id)
      .then((items) => setBranches(items))
      .finally(() => setLoading(false));
  }, [selectedTopic]);

  if (!selectedTopic) {
    return (
      <section className="rounded-xl border border-amber-200 bg-amber-50 p-6 text-sm text-amber-800">
        Please select a topic first.
      </section>
    );
  }

  return (
    <section className="space-y-6">
      <header className="space-y-2">
        <h2 className="text-2xl font-semibold text-slate-900">Choose a Branch</h2>
        <p className="text-sm text-slate-600">
          Showing locations available for {selectedTopic.name}.
        </p>
      </header>

      {loading ? (
        <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Loading branch locations...
        </div>
      ) : (
        <div className="space-y-3">
          {branches.map((branch) => {
            const isSelected = selectedBranch?.id === branch.id;

            return (
              <button
                type="button"
                key={branch.id}
                onClick={() => setSelectedBranch(branch)}
                className={`w-full rounded-xl border bg-white p-4 text-left transition ${
                  isSelected
                    ? 'border-blue-600 ring-2 ring-blue-200'
                    : 'border-slate-200 hover:border-blue-300'
                }`}
              >
                <div className="flex items-start gap-3">
                  <MapPin className="mt-0.5 h-5 w-5 text-blue-700" />
                  <div>
                    <h3 className="font-semibold text-slate-900">{branch.name}</h3>
                    <p className="text-sm text-slate-600">{branch.addressLine1}</p>
                    <p className="text-sm text-slate-600">{branch.addressLine2}</p>
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
          className="rounded-lg border border-slate-300 bg-white px-5 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
        >
          Back
        </button>
        <button
          type="button"
          onClick={goToNextStep}
          disabled={!selectedBranch}
          className="rounded-lg bg-blue-700 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-blue-800 disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          Continue
        </button>
      </div>
    </section>
  );
}
