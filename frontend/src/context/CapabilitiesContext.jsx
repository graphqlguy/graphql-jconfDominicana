import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { fetchCapabilities } from '../graphql/capabilities';
import { buildDocuments } from '../graphql/documents';

const CapabilitiesContext = createContext({ status: 'loading', caps: null, docs: {}, reload: () => {} });

// Introspects the backend schema once at startup and derives every GraphQL
// document the app may send. Pages consult `docs` (null = feature not built
// yet) and `caps` (fine-grained field checks). `reload` re-introspects, e.g.
// after adding a schema field and restarting the backend.
export function CapabilitiesProvider({ children }) {
  const [state, setState] = useState({ status: 'loading', caps: null, docs: {} });

  const reload = useCallback(async () => {
    setState({ status: 'loading', caps: null, docs: {} });
    const caps = await fetchCapabilities();
    if (caps.status !== 'ready') {
      setState({ status: caps.status, caps: null, docs: {} });
      return;
    }
    setState({ status: 'ready', caps, docs: buildDocuments(caps) });
  }, []);

  useEffect(() => { reload(); }, [reload]);

  return (
    <CapabilitiesContext.Provider value={{ ...state, reload }}>
      {children}
    </CapabilitiesContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components -- hook lives alongside its provider by design
export const useCaps = () => useContext(CapabilitiesContext);
