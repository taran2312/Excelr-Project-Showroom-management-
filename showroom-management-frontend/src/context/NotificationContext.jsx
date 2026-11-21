import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';
import Toasts from '../components/Toasts.jsx';

const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const nextId = useRef(1);

  const dismiss = useCallback((id) => {
    setToasts((list) => list.filter((t) => t.id !== id));
  }, []);

  const notify = useCallback(
    (message, variant = 'success', timeoutMs = 4000) => {
      const id = nextId.current++;
      setToasts((list) => [...list, { id, message, variant }]);
      if (timeoutMs) setTimeout(() => dismiss(id), timeoutMs);
    },
    [dismiss],
  );

  const value = useMemo(
    () => ({
      notify,
      success: (msg) => notify(msg, 'success'),
      error: (msg) => notify(msg, 'danger', 6000),
    }),
    [notify],
  );

  return (
    <NotificationContext.Provider value={value}>
      {children}
      <Toasts toasts={toasts} onDismiss={dismiss} />
    </NotificationContext.Provider>
  );
}

export function useNotify() {
  const ctx = useContext(NotificationContext);
  if (!ctx) throw new Error('useNotify must be used inside <NotificationProvider>');
  return ctx;
}
