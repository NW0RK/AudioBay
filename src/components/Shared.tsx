import { ReactNode } from 'react'

export const Icon = ({ name, size = 24, className = '' }: { name: string; size?: number; className?: string }) => {
  const paths: Record<string, string> = {
    home: 'M3 12l9-9 9 9M5 10v10h5v-6h4v6h5V10',
    search: 'M21 21l-4.35-4.35M11 18a7 7 0 100-14 7 7 0 000 14z',
    download: 'M12 3v12m0 0l-5-5m5 5l5-5M5 21h14',
    heart: 'M12 21s-8-5.5-8-11a4.5 4.5 0 019-2 4.5 4.5 0 019 2c0 5.5-8 11-8 11z',
    heartOutline: 'M12 21s-8-5.5-8-11a4.5 4.5 0 019-2 4.5 4.5 0 019 2c0 5.5-8 11-8 11z',
    settings: 'M12 8a4 4 0 100 8 4 4 0 000-8zM19.4 13a7.4 7.4 0 000-2l2-1.5-2-3.5-2.4 1a7.4 7.4 0 00-1.7-1l-.3-2.5H9l-.3 2.5a7.4 7.4 0 00-1.7 1l-2.4-1-2 3.5L4.6 11a7.4 7.4 0 000 2L2.6 14.5l2 3.5 2.4-1a7.4 7.4 0 001.7 1l.3 2.5h5l.3-2.5a7.4 7.4 0 001.7-1l2.4 1 2-3.5-2-1.5z',
    back: 'M15 18l-6-6 6-6',
    more: 'M12 6a2 2 0 110 4 2 2 0 010-4zm0 4a2 2 0 110 4 2 2 0 010-4zm0 4a2 2 0 110 4 2 2 0 010-4z',
    close: 'M6 6l12 12M18 6L6 18',
    pause: 'M9 5v14M15 5v14',
    resume: 'M6 4l12 8-12 8z',
    checkCircle: 'M12 2a10 10 0 100 20 10 10 0 000-20zm5 7l-7 7-3-3',
    error: 'M12 2a10 10 0 100 20 10 10 0 000-20zm0 6v5m0 3h.01',
    pauseCircle: 'M12 2a10 10 0 100 20 10 10 0 000-20zM10 8v8M14 8v8',
    wifiOff: 'M5 12.5a14 14 0 0114 0M2 9a20 20 0 0120 0M8.5 16a7 7 0 017 0M12 20h.01M2 2l20 20',
    fileMusic: 'M14 2v6h6M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z',
    clock: 'M12 2a10 10 0 100 20 10 10 0 000-20zm0 4v6l4 2',
    globe: 'M12 2a10 10 0 100 20 10 10 0 000-20zM2 12h20M12 2a15 15 0 010 20M12 2a15 15 0 000 20',
    key: 'M14 7a4 4 0 11-5.66 5.66L3 18v3h3l3-3M14 7l5 5M14 7l-2-2M17 4l3 3',
    refresh: 'M21 2v6h-6M3 22v-6h6M3 12a9 9 0 0114-7l3 3M21 12a9 9 0 01-14 7l-3-3',
    cloud: 'M6 18a5 5 0 010-10 6 6 0 0111.5 2A4 4 0 0117 18H6z',
    cloudOff: 'M6 18a5 5 0 010-10 6 6 0 0111.5 2A4 4 0 0117 18H6zM2 2l20 20',
    bookOpen: 'M12 6.5C10.5 5 7 4 3 4v15c4 0 7.5 1 9 2.5M12 6.5C13.5 5 17 4 21 4v15c-4 0-7.5 1-9 2.5M12 6.5V22',
    chevronRight: 'M9 6l6 6-6 6',
    sort: 'M3 6h18M6 12h12M9 18h6',
    grid: 'M3 3h8v8H3zM13 3h8v8h-8zM3 13h8v8H3zM13 13h8v8h-8z',
    list: 'M3 5h18M3 12h18M3 19h18',
  }

  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
      strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round"
      className={className} style={{ flexShrink: 0 }}>
      <path d={paths[name] || ''} />
    </svg>
  )
}

export const GlassPanel = ({ children, className = '' }: { children: ReactNode; className?: string }) => (
  <div className={`glass-panel ${className}`}>{children}</div>
)

export const SectionHeading = ({ text }: { text: string }) => (
  <h2 className="section-heading">{text}</h2>
)

export const Badge = ({ tone, children }: { tone: 'success' | 'warning' | 'error' | 'neutral'; children: ReactNode }) => (
  <span className={`badge badge-${tone}`}>
    {children}
  </span>
)

export const MessageState = ({ heading, message, action, onAction, error = false }: {
  heading: string
  message: string
  action: string
  onAction: () => void
  error?: boolean
}) => (
  <div style={{ textAlign: 'center', padding: '32px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
    <Icon name={error ? 'cloudOff' : 'bookOpen'} size={56} className="" />
    <h3 style={{ fontSize: '1.5rem', fontFamily: 'Lora, serif' }}>{heading}</h3>
    <p style={{ fontSize: '0.875rem', color: 'var(--on-surface-variant)' }}>{message}</p>
    <button className="btn-filled" onClick={onAction}>{action}</button>
  </div>
)
