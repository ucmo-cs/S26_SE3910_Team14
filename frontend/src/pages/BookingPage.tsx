import { useState } from 'react';
import Confirmation from '../components/wizard/Confirmation';
import Step1Topics from '../components/wizard/Step1_Topics';
import Step2Locations from '../components/wizard/Step2_Locations';
import Step3DateTime from '../components/wizard/Step3_DateTime';
import Step4Details from '../components/wizard/Step4_Details';
import { useBooking } from '../context/BookingContext';

const steps = [
  { id: 1, label: 'Topic' },
  { id: 2, label: 'Location' },
  { id: 3, label: 'Date & Time' },
  { id: 4, label: 'Details' },
] as const;

export default function BookingPage() {
  const { currentStep } = useBooking();
  const [confirmationNumber, setConfirmationNumber] = useState('');

  const renderCurrentStep = () => {
    if (confirmationNumber) {
      return (
        <Confirmation
          confirmationNumber={confirmationNumber}
          onBookAnother={() => setConfirmationNumber('')}
        />
      );
    }

    switch (currentStep) {
      case 1:
        return <Step1Topics />;
      case 2:
        return <Step2Locations />;
      case 3:
        return <Step3DateTime />;
      case 4:
        return <Step4Details onSubmitted={setConfirmationNumber} />;
      default:
        return null;
    }
  };

  return (
    <main className="min-h-screen bg-slate-100 px-4 py-8 md:px-8">
      <div className="mx-auto w-full max-w-4xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
        <header className="mb-8">
          <h1 className="text-3xl font-semibold text-slate-900">Book an Appointment</h1>
          <p className="mt-2 text-sm text-slate-600">
            Schedule a visit with a banking specialist in four simple steps.
          </p>
        </header>

        {!confirmationNumber ? (
          <div className="mb-8">
            <div className="mb-3 flex justify-between text-xs font-medium text-slate-500">
              {steps.map((step) => (
                <span key={step.id} className={currentStep >= step.id ? 'text-blue-700' : ''}>
                  Step {step.id}: {step.label}
                </span>
              ))}
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-slate-200">
              <div
                className="h-full rounded-full bg-blue-700 transition-all duration-300"
                style={{ width: `${(currentStep / steps.length) * 100}%` }}
              />
            </div>
          </div>
        ) : null}

        {renderCurrentStep()}
      </div>
    </main>
  );
}
