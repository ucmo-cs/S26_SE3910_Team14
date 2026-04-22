import { useEffect, useMemo, useState } from 'react';
import { getAvailableTimeslots } from '../../api/bookingService';
import { useBooking } from '../../context/BookingContext';

function addDays(baseDate: Date, days: number): Date {
  const copy = new Date(baseDate);
  copy.setDate(baseDate.getDate() + days);
  return copy;
}

function formatDate(date: Date): string {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatSlotLabel(slot: string): string {
  const [hoursString, minutesString] = slot.split(':');
  const hours = Number(hoursString);
  const suffix = hours >= 12 ? 'PM' : 'AM';
  const normalizedHours = hours % 12 === 0 ? 12 : hours % 12;
  return `${normalizedHours}:${minutesString} ${suffix}`;
}

export default function Step3DateTime() {
  const [slots, setSlots] = useState<string[]>([]);
  const [unavailableSlots, setUnavailableSlots] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [branchTimeZone, setBranchTimeZone] = useState('');
  const {
    selectedBranch,
    selectedTopic,
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
    if (!selectedBranch || !selectedTopic || !selectedDate) {
      setSlots([]);
      setUnavailableSlots([]);
      return;
    }

    setLoading(true);
    setError('');
    getAvailableTimeslots(selectedBranch.id, selectedTopic.id, selectedDate)
      .then((result) => {
        setSlots(result.slots);
        setUnavailableSlots(result.unavailableSlots ?? []);
        setBranchTimeZone(result.timeZone);
      })
      .catch(() => setError('Unable to load timeslots for this date. Please pick another date or retry.'))
      .finally(() => setLoading(false));
  }, [selectedBranch, selectedTopic, selectedDate]);

  const handleTimePick = (time: string) => {
    setSelectedTime(time);
    goToNextStep();
  };

  if (!selectedBranch) {
    return (
      <section className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-700">
        Please select a branch before choosing date and time.
      </section>
    );
  }

  return (
    <section className="space-y-6 rounded-2xl border border-slate-200 bg-white p-6 md:p-8">
      <header className="space-y-2">
        <h2 className="text-2xl font-semibold text-blue-900">Pick Date and Time</h2>
        <p className="text-sm text-slate-600">
          {branchTimeZone
            ? `Appointments shown in branch timezone: ${branchTimeZone}.`
            : 'Choose a date to load branch availability.'}
        </p>
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
                className={`min-w-28 rounded-full border px-4 py-2 text-sm transition ${
                  isSelected
                    ? 'border-blue-900 bg-blue-900 text-white'
                    : 'border-slate-200 bg-white text-slate-700 hover:border-blue-900'
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
        ) : error ? (
          <div className="rounded-xl border border-red-200 bg-red-50 p-6 text-sm text-red-700">
            {error}
          </div>
        ) : slots.length === 0 ? (
          <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
            No times are available for this date.
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
            {Array.from({ length: 16 }, (_, index) => {
              const hour = 9 + Math.floor(index / 2);
              const minute = index % 2 === 0 ? '00' : '30';
              const slot = `${`${hour}`.padStart(2, '0')}:${minute}`;
              const isAvailable = slots.includes(slot);
              const isUnavailable = unavailableSlots.includes(slot) || !isAvailable;
              const isSelected = selectedTime === slot;
              return (
                <button
                  type="button"
                  key={slot}
                  disabled={!isAvailable}
                  onClick={() => handleTimePick(slot)}
                  className={`rounded-full border px-3 py-2 text-sm transition ${
                    isSelected
                      ? 'border-blue-900 bg-blue-900 text-white'
                      : isUnavailable
                        ? 'border-slate-200 bg-slate-100 text-slate-400'
                        : 'border-slate-200 bg-white text-slate-700 hover:border-blue-900'
                  } disabled:cursor-not-allowed`}
                >
                  {formatSlotLabel(slot)}
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
      </div>
    </section>
  );
}
