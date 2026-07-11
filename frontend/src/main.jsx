import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { ApolloProvider } from '@apollo/client/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CapabilitiesProvider } from './context/CapabilitiesContext';
import client from './apollo/client';
import App from './App';
import './index.css';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ApolloProvider client={client}>
      <BrowserRouter>
        <CapabilitiesProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </CapabilitiesProvider>
      </BrowserRouter>
    </ApolloProvider>
  </StrictMode>
);
