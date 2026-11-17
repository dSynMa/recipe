import React, { useLayoutEffect, useState } from 'react';
import App from './App';

const lockName = 'rcheck-editor';

export default function EditorTab() {
  const [status, setStatus] = useState('opening');

  useLayoutEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    async function openEditor() {
      const holdEditor = () => {
        if (signal.aborted) return;
        setStatus('active');
        return new Promise(resolve => signal.addEventListener('abort', resolve, { once: true }));
      };

      try {
        const locks = navigator.locks;
        await locks.request(lockName, { ifAvailable: true }, async lock => {
          if (lock) return holdEditor();
          if (signal.aborted) return;
          setStatus('waiting');
          await locks.request(lockName, { signal }, holdEditor);
        });
      } catch {
        if (!signal.aborted) setStatus('unavailable');
      }
    }

    openEditor();
    return () => controller.abort();
  }, []);

  if (status === 'active') return <App />;
  if (status === 'opening') return null;
  return (
    <main className="container py-4">
      <h1>R-CHECK</h1>
      <p role="status">{status === 'waiting'
        ? 'The editor is open in another tab. Close that tab to continue here.'
        : 'The editor requires browser tab locking. Open R-CHECK on localhost or HTTPS in a browser that supports Web Locks.'}</p>
    </main>
  );
}
