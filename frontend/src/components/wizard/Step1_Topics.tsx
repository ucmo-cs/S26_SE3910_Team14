import { BadgeDollarSign, Building2, WalletCards } from 'lucide-react';
import { useEffect, useState } from 'react';
import { getTopics } from '../../api/mockService';
import { useBooking, type Topic } from '../../context/BookingContext';

const iconByTopicId = {
  'general-banking': Building2,
  loans: BadgeDollarSign,
  'credit-cards': WalletCards,
} as const;

export default function Step1Topics() {
  const [topics, setTopics] = useState<Topic[]>([]);
  const [loading, setLoading] = useState(true);
  const { selectedTopic, setSelectedTopic, goToNextStep } = useBooking();

  useEffect(() => {
    getTopics()
      .then((items) => setTopics(items))
      .finally(() => setLoading(false));
  }, []);

  return (
    <section className="space-y-6">
      <header className="space-y-2">
        <h2 className="text-2xl font-semibold text-slate-900">Select a Service Topic</h2>
        <p className="text-sm text-slate-600">
          Choose the type of appointment you would like to schedule.
        </p>
      </header>

      {loading ? (
        <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Loading available topics...
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          {topics.map((topic) => {
            const Icon = iconByTopicId[topic.id as keyof typeof iconByTopicId] ?? Building2;
            const isSelected = selectedTopic?.id === topic.id;

            return (
              <button
                type="button"
                key={topic.id}
                onClick={() => setSelectedTopic(topic)}
                className={`rounded-xl border bg-white p-5 text-left transition ${
                  isSelected
                    ? 'border-blue-600 ring-2 ring-blue-200'
                    : 'border-slate-200 hover:border-blue-300'
                }`}
              >
                <Icon className="mb-4 h-8 w-8 text-blue-700" />
                <h3 className="text-base font-semibold text-slate-900">{topic.name}</h3>
                <p className="mt-2 text-sm text-slate-600">{topic.description}</p>
              </button>
            );
          })}
        </div>
      )}

      <div className="flex justify-end">
        <button
          type="button"
          onClick={goToNextStep}
          disabled={!selectedTopic}
          className="rounded-lg bg-blue-700 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-blue-800 disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          Continue
        </button>
      </div>
    </section>
  );
}
