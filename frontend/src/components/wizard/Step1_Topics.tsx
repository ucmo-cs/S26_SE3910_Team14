import { CreditCard, Home, Landmark } from 'lucide-react';
import { useEffect, useState } from 'react';
import { getTopics } from '../../api/bookingService';
import { useBooking, type Topic } from '../../context/BookingContext';

export default function Step1Topics() {
  const [topics, setTopics] = useState<Topic[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { selectedTopic, setSelectedTopic, goToNextStep } = useBooking();

  const loadTopics = () => {
    setLoading(true);
    setError('');
    getTopics()
      .then((items) => setTopics(items))
      .catch(() => setError('Unable to load appointment topics. Please try again.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadTopics();
  }, []);

  const chooseTopic = (topic: Topic) => {
    setSelectedTopic(topic);
    goToNextStep();
  };

  const getTopicIcon = (topic: Topic) => {
    const normalized = `${topic.code} ${topic.name}`.toLowerCase();
    if (normalized.includes('mortgage') || normalized.includes('loan')) {
      return Home;
    }
    if (normalized.includes('card') || normalized.includes('credit')) {
      return CreditCard;
    }
    return Landmark;
  };

  return (
    <section className="space-y-6 rounded-2xl border border-slate-200 bg-white p-6 md:p-8">
      <header className="space-y-2">
        <h2 className="text-2xl font-semibold text-blue-900">Choose Your Appointment Type</h2>
        <p className="text-sm text-slate-600">
          Choose the type of appointment you would like to schedule.
        </p>
      </header>

      {loading ? (
        <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Loading available topics...
        </div>
      ) : error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 p-6 text-sm text-red-700">
          <p>{error}</p>
          <button
            type="button"
            onClick={loadTopics}
            className="mt-3 rounded-md bg-blue-900 px-3 py-2 text-xs font-medium text-white transition hover:bg-blue-800"
          >
            Retry
          </button>
        </div>
      ) : topics.length === 0 ? (
        <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-500">
          No appointment topics are available right now.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {topics.map((topic) => {
            const Icon = getTopicIcon(topic);
            const isSelected = selectedTopic?.id === topic.id;

            return (
              <button
                type="button"
                key={topic.id}
                onClick={() => chooseTopic(topic)}
                className={`aspect-square rounded-2xl border bg-white p-5 text-left transition ${
                  isSelected
                    ? 'border-blue-900 ring-2 ring-slate-300'
                    : 'border-slate-200 hover:border-blue-900'
                }`}
              >
                <Icon className="mb-4 h-8 w-8 text-blue-900" />
                <h3 className="text-base font-semibold text-blue-900">{topic.name}</h3>
                <p className="mt-2 line-clamp-3 text-sm text-slate-600">{topic.description}</p>
              </button>
            );
          })}
        </div>
      )}
    </section>
  );
}
