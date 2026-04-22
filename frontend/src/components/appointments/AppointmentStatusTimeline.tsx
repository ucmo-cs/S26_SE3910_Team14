type AppointmentStatusTimelineProps = {
  status: string;
};

export default function AppointmentStatusTimeline({ status }: AppointmentStatusTimelineProps) {
  return (
    <div className="mt-2">
      <p className="text-xs font-medium text-slate-600">Current Status: {status.toUpperCase()}</p>
    </div>
  );
}
