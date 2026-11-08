import { useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import ToastHost from './ToastHost';

export default function Layout() {
  const [open, setOpen] = useState(false);
  const { pathname } = useLocation();

  useEffect(() => setOpen(false), [pathname]);

  return (
    <>
      <div className="bg-aurora" aria-hidden="true"><div className="bg-dots" /></div>
      <div className="app">
        <Sidebar open={open} onNavigate={() => setOpen(false)} />
        <div className={`scrim ${open ? 'show' : ''}`} onClick={() => setOpen(false)} aria-hidden="true" />
        <main className="main">
          <Topbar onMenu={() => setOpen(true)} />
          <div key={pathname} className="page">
            <Outlet />
          </div>
        </main>
      </div>
      <ToastHost />
    </>
  );
}
