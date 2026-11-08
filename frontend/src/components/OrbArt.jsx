import { useId } from 'react';

/** Quả cầu 3D bóng kiểu Vision: cầu tối + hai vành bạc quay ngược chiều + quầng sáng Emerald. */
export default function OrbArt({ className = '' }) {
  const uid = useId().replace(/:/g, '');
  return (
    <svg className={`orb ${className}`} viewBox="0 0 400 400" aria-hidden="true" focusable="false">
      <defs>
        <radialGradient id={`${uid}-sphere`} cx="35%" cy="30%" r="78%">
          <stop offset="0" stopColor="#566069" />
          <stop offset="0.42" stopColor="#1b2126" />
          <stop offset="1" stopColor="#040608" />
        </radialGradient>
        <linearGradient id={`${uid}-ring`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#f3f6f8" stopOpacity="0.95" />
          <stop offset="0.5" stopColor="#7d8892" stopOpacity="0.7" />
          <stop offset="1" stopColor="#20c6ae" stopOpacity="0.4" />
        </linearGradient>
        <linearGradient id={`${uid}-ring2`} x1="1" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#39bde0" stopOpacity="0.7" />
          <stop offset="1" stopColor="#c9d2d9" stopOpacity="0.15" />
        </linearGradient>
        <radialGradient id={`${uid}-glow`}>
          <stop offset="0" stopColor="#20c6ae" stopOpacity="0.5" />
          <stop offset="1" stopColor="#20c6ae" stopOpacity="0" />
        </radialGradient>
      </defs>
      <circle className="glow" cx="200" cy="200" r="190" fill={`url(#${uid}-glow)`} />
      <g className="ring-b">
        <ellipse cx="200" cy="200" rx="178" ry="60" fill="none" stroke={`url(#${uid}-ring2)`} strokeWidth="9"
          transform="rotate(-28 200 200)" />
      </g>
      <circle cx="200" cy="200" r="104" fill={`url(#${uid}-sphere)`} />
      <ellipse cx="168" cy="152" rx="44" ry="24" fill="#ffffff" opacity="0.11" transform="rotate(-24 168 152)" />
      <g className="ring-a">
        <ellipse cx="200" cy="200" rx="150" ry="46" fill="none" stroke={`url(#${uid}-ring)`} strokeWidth="15"
          transform="rotate(24 200 200)" />
      </g>
    </svg>
  );
}
