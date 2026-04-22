import {
  createContext,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react';

export type Topic = {
  id: number;
  code: string;
  name: string;
  description: string;
  defaultDurationMinutes: number;
};

export type Branch = {
  id: number;
  displayName: string;
  streetLine1: string;
  streetLine2: string | null;
  city: string | null;
  stateProvince: string | null;
  postalCode: string | null;
  timeZone: string;
};

export type CustomerDetails = {
  firstName: string;
  lastName: string;
  email: string;
};

export type BookingContextValue = {
  currentStep: 1 | 2 | 3 | 4;
  selectedTopic: Topic | null;
  selectedBranch: Branch | null;
  selectedDate: string | null;
  selectedTime: string | null;
  customerDetails: CustomerDetails;
  setCurrentStep: (step: 1 | 2 | 3 | 4) => void;
  goToNextStep: () => void;
  goToPreviousStep: () => void;
  setSelectedTopic: (topic: Topic | null) => void;
  setSelectedBranch: (branch: Branch | null) => void;
  setSelectedDate: (date: string | null) => void;
  setSelectedTime: (time: string | null) => void;
  setCustomerDetails: (details: CustomerDetails) => void;
  resetBooking: () => void;
};

const defaultCustomerDetails: CustomerDetails = {
  firstName: '',
  lastName: '',
  email: '',
};

const BookingContext = createContext<BookingContextValue | undefined>(undefined);

export function BookingProvider({ children }: { children: ReactNode }) {
  const [currentStep, setCurrentStep] = useState<1 | 2 | 3 | 4>(1);
  const [selectedTopic, setSelectedTopicState] = useState<Topic | null>(null);
  const [selectedBranch, setSelectedBranchState] = useState<Branch | null>(null);
  const [selectedDate, setSelectedDateState] = useState<string | null>(null);
  const [selectedTime, setSelectedTimeState] = useState<string | null>(null);
  const [customerDetails, setCustomerDetailsState] =
    useState<CustomerDetails>(defaultCustomerDetails);

  const setSelectedTopic = (topic: Topic | null) => {
    setSelectedTopicState(topic);
    setSelectedBranchState(null);
    setSelectedDateState(null);
    setSelectedTimeState(null);
  };

  const setSelectedBranch = (branch: Branch | null) => {
    setSelectedBranchState(branch);
    setSelectedDateState(null);
    setSelectedTimeState(null);
  };

  const setSelectedDate = (date: string | null) => {
    setSelectedDateState(date);
    setSelectedTimeState(null);
  };

  const goToNextStep = () => {
    setCurrentStep((prev) => (prev < 4 ? ((prev + 1) as 1 | 2 | 3 | 4) : prev));
  };

  const goToPreviousStep = () => {
    setCurrentStep((prev) => (prev > 1 ? ((prev - 1) as 1 | 2 | 3 | 4) : prev));
  };

  const resetBooking = () => {
    setCurrentStep(1);
    setSelectedTopicState(null);
    setSelectedBranchState(null);
    setSelectedDateState(null);
    setSelectedTimeState(null);
    setCustomerDetailsState(defaultCustomerDetails);
  };

  const value = useMemo<BookingContextValue>(
    () => ({
      currentStep,
      selectedTopic,
      selectedBranch,
      selectedDate,
      selectedTime,
      customerDetails,
      setCurrentStep,
      goToNextStep,
      goToPreviousStep,
      setSelectedTopic,
      setSelectedBranch,
      setSelectedDate,
      setSelectedTime: setSelectedTimeState,
      setCustomerDetails: setCustomerDetailsState,
      resetBooking,
    }),
    [currentStep, selectedTopic, selectedBranch, selectedDate, selectedTime, customerDetails],
  );

  return <BookingContext.Provider value={value}>{children}</BookingContext.Provider>;
}

export function useBooking(): BookingContextValue {
  const context = useContext(BookingContext);
  if (!context) {
    throw new Error('useBooking must be used within BookingProvider');
  }

  return context;
}
