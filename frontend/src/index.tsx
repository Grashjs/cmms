import ReactDOM from 'react-dom';
import { Helmet, HelmetProvider } from 'react-helmet-async';
import { BrowserRouter } from 'react-router-dom';
import ScrollTop from 'src/hooks/useScrollTop';

import 'nprogress/nprogress.css';
import { Provider } from 'react-redux';
import store from 'src/store';
import App from 'src/App';
import { SidebarProvider } from 'src/contexts/SidebarContext';
import { TitleProvider } from 'src/contexts/TitleContext';
import * as serviceWorker from 'src/serviceWorker';
import { AuthProvider } from 'src/contexts/JWTAuthContext';
import {
  sentryDsn,
  sentryEnvironment,
  sentryRelease,
  zendeskKey
} from './config';
import { ZendeskProvider } from 'react-use-zendesk';
import * as Sentry from '@sentry/react';

Sentry.init({
  dsn: sentryDsn,
  environment: sentryEnvironment,
  release: sentryRelease,
  tracesSampleRate: 0.1,
  integrations: [Sentry.browserTracingIntegration()]
});

ReactDOM.render(
  <HelmetProvider>
    <Helmet>
      <meta name="robots" content="noindex, nofollow" />
    </Helmet>
    <Provider store={store}>
      <SidebarProvider>
        <TitleProvider>
          <BrowserRouter>
            <ScrollTop />
            <ZendeskProvider apiKey={zendeskKey}>
              <AuthProvider>
                <Sentry.ErrorBoundary
                  fallback={<div>Something went wrong.</div>}
                >
                  <App />
                </Sentry.ErrorBoundary>
              </AuthProvider>
            </ZendeskProvider>
          </BrowserRouter>
        </TitleProvider>
      </SidebarProvider>
    </Provider>
  </HelmetProvider>,
  document.getElementById('root')
);

serviceWorker.unregister();
