import { useState, type FormEvent } from 'react';
import { submitAppointment } from '../../api/mockService';
import { useBooking } from '../../context/BookingContext';

type Step4DetailsProps = {
  onSubmitted: (confirmationNumber: string) => void;
};

export default function Step4Details({ onSubmitted }: Step4DetailsProps) {
  const {
    selectedTopic,
    selectedBranch,
    selectedDate,
    selectedTime,
    customerDetails,
    setCustomerDetails,
    goToPreviousStep,
  } = useBooking();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const canSubmit =
    selectedTopic &&
    selectedBranch &&
    selectedDate &&
    selectedTime &&
    customerDetails.firstName &&
    customerDetails.lastName &&
    customerDetails.email;

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!canSubmit) {
      return;
    }

    try {
      setSubmitting(true);
      setError('');
      const response = await submitAppointment({
        topic: selectedTopic,
        branch: selectedBranch,
        date: selectedDate,
        time: selectedTime,
        customerDetails,
      });
      onSubmitted(response.confirmationNumber);
    } catch {
      setError('Unable to submit your appointment right now. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="space-y-6">
      <header className="space-y-2">
        <h2 className="text-2xl font-semibold text-slate-900">Your Details</h2>
        <p className="text-sm text-slate-600">Provide your contact information to confirm.</p>
      </header>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <label className="space-y-1 text-sm">
            <span className="font-medium text-slate-700">First Name</span>
            <input
              type="text"
              value={customerDetails.firstName}
              onChange={(event) =>
                setCustomerDetails({ ...customerDetails, firstName: event.target.value })
              }
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
              required
            />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-medium text-slate-700">Last Name</span>
            <input
              type="text"
              value={customerDetails.lastName}
              onChange={(event) =>
                setCustomerDetails({ ...customerDetails, lastName: event.target.value })
              }
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
              required
            />
          </label>
        </div>

        <label className="space-y-1 text-sm">
          <span className="font-medium text-slate-700">Email Address</span>
          <input
            type="email"
            value={customerDetails.email}
            onChange={(event) => setCustomerDetails({ ...customerDetails, email: event.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
            required
          />
        </label>

        {error ? (
          <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        ) : null}

        <div className="flex items-center justify-between">
          <button
            type="button"
            onClick={goToPreviousStep}
            className="rounded-lg border border-slate-300 bg-white px-5 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          >
            Back
          </button>
          <button
            type="submit"
            disabled={!canSubmit || submitting}
            className="rounded-lg bg-blue-700 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-blue-800 disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {submitting ? 'Submitting...' : 'Confirm Appointment'}
          </button>
        </div>
      </form>
    </section>
  );
}
