const flow = ['REQUESTED', 'APPROVED', 'COMPLETED', 'CANCELLED'] as const;

type AppointmentStatusTimelineProps = {
  status: string;
};

export default function AppointmentStatusTimeline({ status }: AppointmentStatusTimelineProps) {
  const normalized = status.toUpperCase();
  const statusIndex = flow.indexOf(normalized as (typeof flow)[number]);
  const isCancelled = normalized === 'CANCELLED';

  return (
    <div className="mt-2 space-y-2">
      <div className="flex flex-wrap items-center gap-2 text-xs">
        {flow.map((step, index) => {
          const reached = !isCancelled && statusIndex >= index;
          const cancelledFlag = isCancelled && step === 'CANCELLED';
          return (
            <span
              key={step}
              className={`rounded-full border px-2 py-0.5 ${
                cancelledFlag
                  ? 'border-red-200 bg-red-50 text-red-700'
                  : reached
                    ? 'border-blue-900 bg-blue-900 text-white'
                    : 'border-slate-200 bg-white text-slate-500'
              }`}
            >
              {step}
            </span>
          );
        })}
      </div>
      {isCancelled ? (
        <p className="text-xs text-red-600">
          Cancelled appointments are no longer eligible for completion workflow.
        </p>
      ) : null}
    </div>
  );
}
