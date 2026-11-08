import { useEffect, useState } from 'react';

// Một bộ đếm 1 giây dùng chung cho mọi component (đồng hồ, "x giây trước").
const subs = new Set();
let timer = null;

function start() {
  if (timer) return;
  timer = setInterval(() => subs.forEach((fn) => fn(Date.now())), 1000);
}
function stop() {
  if (subs.size === 0 && timer) {
    clearInterval(timer);
    timer = null;
  }
}

export function useNow() {
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    subs.add(setNow);
    start();
    return () => {
      subs.delete(setNow);
      stop();
    };
  }, []);
  return now;
}
