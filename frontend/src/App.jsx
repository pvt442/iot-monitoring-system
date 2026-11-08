import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { DataProvider } from './context/DataContext';
import Layout from './components/Layout';
import DashboardPage from './pages/DashboardPage';
import DevicesPage from './pages/DevicesPage';
import HistoryPage from './pages/HistoryPage';
import AlertLogsPage from './pages/AlertLogsPage';
import ConfigurationPage from './pages/ConfigurationPage';

export default function App() {
  return (
    <BrowserRouter>
      <DataProvider>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<DashboardPage />} />
            <Route path="devices" element={<DevicesPage />} />
            <Route path="history" element={<HistoryPage />} />
            <Route path="alerts" element={<AlertLogsPage />} />
            <Route path="config" element={<ConfigurationPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </DataProvider>
    </BrowserRouter>
  );
}
