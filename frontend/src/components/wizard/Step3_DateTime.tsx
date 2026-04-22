import { useEffect, useMemo, useState } from 'react';
import { getAvailableTimeslots } from '../../api/mockService';
import { useBooking } from '../../context/BookingContext';

function addDays(baseDate: Date, days: number): Date {
  const copy = new Date(baseDate);
  copy.setDate(baseDate.getDate() + days);
  return copy;
}

function formatDate(date: Date): string {
  return date.toISOString().split('T')[0];
}

export default function Step3DateTime() {
  const [slots, setSlots] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const {
    selectedBranch,
    selectedDate,
    selectedTime,
    setSelectedDate,
    setSelectedTime,
    goToNextStep,
    goToPreviousStep,
  } = useBooking();

  const dateOptions = useMemo(
    () => Array.from({ length: 7 }, (_, index) => addDays(new Date(), index)),
    [],
  );

  useEffect(() => {
    if (!selectedBranch || !selectedDate) {
      setSlots([]);
      return;
    }

    setLoading(true);
    getAvailableTimeslots(selectedBranch.id, selectedDate)
      .then((result) => setSlots(result))
      .finally(() => setLoading(false));
  }, [selectedBranch, selectedDate]);

  if (!selectedBranch) {
    return (
      <section className="rounded-xl border border-amber-200 bg-amber-50 p-6 text-sm text-amber-800">
        Please select a branch before choosing date and time.
      </section>
    );
  }

  return (
    <section className="space-y-6">
      <header className="space-y-2">
        <h2 className="text-2xl font-semibold text-slate-900">Select Date and Time</h2>
        <p className="text-sm text-slate-600">Appointments are shown in your local timezone.</p>
      </header>

      <div>
        <p className="mb-3 text-sm font-medium text-slate-700">Available Dates</p>
        <div className="flex gap-3 overflow-x-auto pb-2">
          {dateOptions.map((date) => {
            const isoDate = formatDate(date);
            const isSelected = selectedDate === isoDate;
            const label = date.toLocaleDateString(undefined, {
              weekday: 'short',
              month: 'short',
              day: 'numeric',
            });

            return (
              <button
                type="button"
                key={isoDate}
                onClick={() => setSelectedDate(isoDate)}
                className={`min-w-28 rounded-lg border px-4 py-2 text-sm transition ${
                  isSelected
                    ? 'border-blue-600 bg-blue-50 text-blue-800'
                    : 'border-slate-200 bg-white text-slate-700 hover:border-blue-300'
                }`}
              >
                {label}
              </button>
            );
          })}
        </div>
      </div>

      <div>
        <p className="mb-3 text-sm font-medium text-slate-700">Available Timeslots</p>
        {loading ? (
          <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
            Loading timeslots...
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
            {slots.map((slot) => {
              const isSelected = selectedTime === slot;
              return (
                <button
                  type="button"
                  key={slot}
                  onClick={() => setSelectedTime(slot)}
                  className={`rounded-lg border px-3 py-2 text-sm transition ${
                    isSelected
                      ? 'border-blue-600 bg-blue-50 text-blue-800'
                      : 'border-slate-200 bg-white text-slate-700 hover:border-blue-300'
                  }`}
                >
                  {slot}
                </button>
              );
            })}
          </div>
        )}
      </div>

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
          disabled={!selectedDate || !selectedTime}
          className="rounded-lg bg-blue-700 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-blue-800 disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          Continue
        </button>
      </div>
    </section>
  );
}
