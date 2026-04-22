import { useEffect, useMemo, useState } from 'react';
import { getAvailableTimeslots } from '../../api/bookingService';
import { useBooking } from '../../context/BookingContext';

function addDays(baseDate: Date, days: number): Date {
  const copy = new Date(baseDate);
  copy.setDate(baseDate.getDate() + days);
  return copy;
}

function isWeekday(date: Date): boolean {
  const day = date.getDay();
  return day >= 1 && day <= 5;
}

function nextBusinessDates(count: number): Date[] {
  const output: Date[] = [];
  let offset = 0;
  while (output.length < count) {
    const candidate = addDays(new Date(), offset);
    if (isWeekday(candidate)) {
      output.push(candidate);
    }
    offset += 1;
  }
  return output;
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
    selectedDurationMinutes,
    setSelectedDate,
    setSelectedTime,
    setSelectedDurationMinutes,
    goToNextStep,
    goToPreviousStep,
  } = useBooking();

  const dateOptions = useMemo(
    () => nextBusinessDates(7),
    [],
  );
  const displayedSlots = useMemo(
    () => [...new Set([...slots, ...unavailableSlots])].sort((a, b) => a.localeCompare(b)),
    [slots, unavailableSlots],
  );

  useEffect(() => {
    if (!selectedBranch || !selectedTopic || !selectedDate) {
      setSlots([]);
      setUnavailableSlots([]);
      return;
    }

    setLoading(true);
    setError('');
    getAvailableTimeslots(selectedBranch.id, selectedTopic.id, selectedDate, selectedDurationMinutes)
      .then((result) => {
        setSlots(result.slots);
        setUnavailableSlots(result.unavailableSlots ?? []);
        setBranchTimeZone(result.timeZone);
      })
      .catch(() => setError('Unable to load timeslots for this date. Please pick another date or retry.'))
      .finally(() => setLoading(false));
  }, [selectedBranch, selectedTopic, selectedDate, selectedDurationMinutes]);

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
        <div className="mb-3 flex gap-2">
          <button
            type="button"
            onClick={() => setSelectedDurationMinutes(30)}
            className={`rounded-full border px-3 py-1 text-xs ${
              selectedDurationMinutes === 30
                ? 'border-blue-900 bg-blue-900 text-white'
                : 'border-slate-200 bg-white text-slate-700'
            }`}
          >
            30-minute appointment
          </button>
          <button
            type="button"
            onClick={() => setSelectedDurationMinutes(60)}
            className={`rounded-full border px-3 py-1 text-xs ${
              selectedDurationMinutes === 60
                ? 'border-blue-900 bg-blue-900 text-white'
                : 'border-slate-200 bg-white text-slate-700'
            }`}
          >
            60-minute appointment
          </button>
        </div>
        {loading ? (
          <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
            Loading timeslots...
          </div>
        ) : error ? (
          <div className="rounded-xl border border-red-200 bg-red-50 p-6 text-sm text-red-700">
            {error}
          </div>
        ) : displayedSlots.length === 0 ? (
          <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
            No times are available for this date.
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
            {displayedSlots.map((slot) => {
              const isSelected = selectedTime === slot;
              const isAvailable = slots.includes(slot);
              return (
                <button
                  type="button"
                  key={slot}
                  disabled={!isAvailable}
                  onClick={() => handleTimePick(slot)}
                  className={`rounded-full border px-3 py-2 text-sm transition ${
                    isSelected
                      ? 'border-blue-900 bg-blue-900 text-white'
                      : isAvailable
                        ? 'border-slate-200 bg-white text-slate-700 hover:border-blue-900'
                        : 'border-slate-200 bg-slate-100 text-slate-400'
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
