import { CircleCheckBig } from 'lucide-react';
import { useBooking } from '../../context/BookingContext';

type ConfirmationProps = {
  confirmationNumber: string;
  onBookAnother: () => void;
};

export default function Confirmation({ confirmationNumber, onBookAnother }: ConfirmationProps) {
  const { selectedTopic, selectedBranch, selectedDate, selectedTime, customerDetails, resetBooking } =
    useBooking();

  return (
    <section className="space-y-6">
      <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-6 text-center">
        <CircleCheckBig className="mx-auto mb-4 h-12 w-12 text-emerald-600" />
        <h2 className="text-2xl font-semibold text-slate-900">Appointment Confirmed</h2>
        <p className="mt-2 text-sm text-slate-600">Your reservation has been successfully submitted.</p>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-6">
        <h3 className="text-base font-semibold text-slate-900">Appointment Summary</h3>
        <dl className="mt-4 grid grid-cols-1 gap-y-3 text-sm text-slate-700 md:grid-cols-2">
          <div>
            <dt className="font-medium text-slate-500">Confirmation Number</dt>
            <dd>{confirmationNumber}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Customer</dt>
            <dd>{customerDetails.firstName} {customerDetails.lastName}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Email</dt>
            <dd>{customerDetails.email}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Topic</dt>
            <dd>{selectedTopic?.name}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Branch</dt>
            <dd>{selectedBranch?.name}</dd>
          </div>
          <div>
            <dt className="font-medium text-slate-500">Date and Time</dt>
            <dd>{selectedDate} at {selectedTime}</dd>
          </div>
        </dl>
      </div>

      <div className="flex justify-end">
        <button
          type="button"
          onClick={() => {
            resetBooking();
            onBookAnother();
          }}
          className="rounded-lg bg-blue-700 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-blue-800"
        >
          Book Another Appointment
        </button>
      </div>
    </section>
  );
}
