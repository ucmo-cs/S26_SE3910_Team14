import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AuthProvider } from './context/AuthProvider';
import { BookingProvider } from './context/BookingContext';
import App from './App';
import './style.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <BookingProvider>
        <App />
      </BookingProvider>
    </AuthProvider>
  </StrictMode>,
);
