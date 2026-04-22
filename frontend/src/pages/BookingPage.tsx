import { useEffect, useRef, useState } from 'react';
import NavBar from '../components/NavBar';
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
  const { currentStep, setCurrentStep, selectedTopic, selectedBranch, selectedDate, selectedTime } =
    useBooking();
  const [confirmationNumber, setConfirmationNumber] = useState('');
  const carouselRef = useRef<HTMLDivElement | null>(null);
  const step1Ref = useRef<HTMLDivElement | null>(null);
  const step2Ref = useRef<HTMLDivElement | null>(null);
  const step3Ref = useRef<HTMLDivElement | null>(null);
  const step4Ref = useRef<HTMLDivElement | null>(null);
  const successRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const targetRef = confirmationNumber
      ? successRef
      : currentStep === 1
        ? step1Ref
        : currentStep === 2
          ? step2Ref
          : currentStep === 3
            ? step3Ref
            : step4Ref;

    targetRef.current?.scrollIntoView({
      behavior: 'smooth',
      block: 'nearest',
      inline: 'start',
    });
  }, [currentStep, confirmationNumber]);

  const restartFlow = () => {
    setConfirmationNumber('');
    setCurrentStep(1);
  };

  const progressWidth = confirmationNumber ? 100 : (currentStep / steps.length) * 100;

  const highestUnlockedStep = !selectedTopic
    ? 1
    : !selectedBranch
      ? 2
      : !selectedDate || !selectedTime
        ? 3
        : 4;

  const jumpToStep = (step: 1 | 2 | 3 | 4) => {
    if (confirmationNumber) {
      return;
    }
    if (step > highestUnlockedStep) {
      return;
    }
    setCurrentStep(step);
  };

  return (
    <main className="min-h-screen bg-slate-100">
      <NavBar />
      <div className="mx-auto mt-8 w-full max-w-5xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
        <header className="mb-8">
          <h1 className="text-3xl font-semibold text-blue-900">Central Bank Appointment Booking</h1>
          <p className="mt-2 text-sm text-slate-600">
            Complete your reservation through our four-step card roulette wizard.
          </p>
        </header>

        <div className="mb-6">
          <div className="mb-3 flex flex-wrap gap-2 text-xs font-medium">
            {steps.map((step) => (
              <button
                key={step.id}
                type="button"
                onClick={() => jumpToStep(step.id)}
                disabled={!confirmationNumber && step.id > highestUnlockedStep}
                className={`rounded-full border px-3 py-1 transition ${
                  currentStep >= step.id || (confirmationNumber && step.id === 4)
                    ? 'border-blue-900 bg-blue-900 text-white'
                    : 'border-slate-200 bg-white text-slate-600'
                } disabled:cursor-not-allowed disabled:opacity-50`}
              >
                Step {step.id}: {step.label}
              </button>
            ))}
            {confirmationNumber ? (
              <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-slate-600">
                Success
              </span>
            ) : null}
          </div>
          <div className="h-2 overflow-hidden rounded-full bg-slate-200">
            <div
              className="h-full rounded-full bg-blue-900 transition-all duration-500"
              style={{ width: `${progressWidth}%` }}
            />
          </div>
        </div>

        <div
          ref={carouselRef}
          className="flex snap-x snap-mandatory gap-4 overflow-x-hidden scroll-smooth"
          aria-live="polite"
        >
          <div ref={step1Ref} className="min-w-full snap-center">
            <Step1Topics />
          </div>
          <div ref={step2Ref} className="min-w-full snap-center">
            <Step2Locations />
          </div>
          <div ref={step3Ref} className="min-w-full snap-center">
            <Step3DateTime />
          </div>
          <div ref={step4Ref} className="min-w-full snap-center">
            <Step4Details onSubmitted={setConfirmationNumber} />
          </div>
          <div ref={successRef} className="min-w-full snap-center">
            {confirmationNumber ? (
              <Confirmation confirmationNumber={confirmationNumber} onBookAnother={restartFlow} />
            ) : (
              <section className="rounded-2xl border border-slate-200 bg-white p-8 text-center text-slate-500">
                Confirmation card will appear after submission.
              </section>
            )}
          </div>
        </div>
      </div>
    </main>
  );
}
