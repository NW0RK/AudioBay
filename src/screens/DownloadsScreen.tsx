import { downloads } from '../mockData'
import { Icon, GlassPanel, SectionHeading, Badge, MessageState } from '../components/Shared'
import { useNavigate } from 'react-router-dom'
import { useState } from 'react'

const statusLabel = (s: string): { text: string; tone: 'success' | 'warning' | 'error' | 'neutral' } => {
  const map: Record<string, { text: string; tone: 'success' | 'warning' | 'error' | 'neutral' }> = {
    downloading: { text: 'Downloading', tone: 'neutral' },
    completed: { text: 'Completed', tone: 'success' },
    paused: { text: 'Paused', tone: 'warning' },
    waiting_wifi: { text: 'Waiting for Wi-Fi', tone: 'warning' },
    error: { text: 'Download failed', tone: 'error' },
    queued: { text: 'Finding peers and metadata', tone: 'neutral' },
    checking: { text: 'Checking files', tone: 'neutral' },
    publishing: { text: 'Saving and verifying shared files', tone: 'neutral' },
  }
  return map[s] || { text: s, tone: 'neutral' }
}

export const DownloadsScreen = () => {
  const navigate = useNavigate()
  const [expanded, setExpanded] = useState(true)

  return (
    <div className="page">
      <div style={{ display: 'flex', alignItems: 'center', padding: '8px 16px', position: 'sticky', top: 0, background: 'var(--surface)', zIndex: 10 }}>
        <h1 className="text-title-md" style={{ fontSize: '1.125rem' }}>Downloads</h1>
      </div>
      <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <GlassPanel>
          <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <button onClick={() => setExpanded(!expanded)} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
              <span className="text-title-md">Set up a download</span>
              <Icon name="back" size={20} className="" />
            </button>
            {expanded && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <p className="text-body-sm">Add a magnet link or torrent file</p>
                <div>
                  <label className="text-body-sm" style={{ display: 'block', marginBottom: '4px' }}>Author (required)</label>
                  <input style={{ width: '100%', padding: '12px', borderRadius: '12px', background: 'var(--surface-container)', color: 'var(--on-surface)', border: '1px solid var(--outline-variant)', outline: 'none' }} placeholder="Author" />
                </div>
                <div>
                  <label className="text-body-sm" style={{ display: 'block', marginBottom: '4px' }}>Book title (required)</label>
                  <input style={{ width: '100%', padding: '12px', borderRadius: '12px', background: 'var(--surface-container)', color: 'var(--on-surface)', border: '1px solid var(--outline-variant)', outline: 'none' }} placeholder="Book title" />
                </div>
                <div>
                  <label className="text-body-sm" style={{ display: 'block', marginBottom: '4px' }}>Magnet link</label>
                  <input style={{ width: '100%', padding: '12px', borderRadius: '12px', background: 'var(--surface-container)', color: 'var(--on-surface)', border: '1px solid var(--outline-variant)', outline: 'none' }} placeholder="magnet:?xt=urn:btih:..." />
                </div>
                <div>
                  <label className="text-body-sm" style={{ display: 'block', marginBottom: '4px' }}>Audiobook folder</label>
                  <button className="btn-tonal" style={{ width: '100%', justifyContent: 'space-between' }}>
                    <span>Choose audiobook folder</span>
                    <Icon name="chevronRight" size={20} />
                  </button>
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button className="btn-filled" style={{ flex: 1 }}>Start download</button>
                  <button className="btn-tonal" style={{ flex: 1 }}>Open torrent</button>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '12px', background: 'var(--surface-container)', borderRadius: '12px' }}>
                  <Icon name="wifiOff" size={20} />
                  <div style={{ flex: 1 }}>
                    <p className="text-label-md">Download over Wi-Fi only</p>
                    <p className="text-body-sm">Transfers pause on mobile data.</p>
                  </div>
                  <div style={{ width: '44px', height: '24px', borderRadius: '12px', background: 'var(--primary)', position: 'relative' }}>
                    <div style={{ position: 'absolute', right: '2px', top: '2px', width: '20px', height: '20px', borderRadius: '50%', background: 'var(--on-primary)' }} />
                  </div>
                </div>
              </div>
            )}
          </div>
        </GlassPanel>
        <SectionHeading text="Download queue" />
        {downloads.map((dl) => {
          const label = statusLabel(dl.status)
          return (
            <GlassPanel key={dl.id}>
              <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <h3 style={{ fontSize: '1.375rem', fontFamily: 'Lora, serif' }}>{dl.title}</h3>
                <p style={{ color: 'var(--on-surface-variant)' }}>{dl.author}</p>
                <Badge tone={label.tone}>
                  <Icon name={label.tone === 'success' ? 'checkCircle' : label.tone === 'warning' ? 'pauseCircle' : label.tone === 'error' ? 'error' : 'fileMusic'} size={18} />
                  {label.text}
                </Badge>
                <div className="progress-bar"><div style={{ width: `${dl.progress * 100}%` }} /></div>
                {dl.status === 'downloading' && (
                  <p className="text-body-sm">{Math.round(dl.progress * 100)}% · {dl.speed} KB/s · {dl.peers} peers</p>
                )}
                {dl.status === 'downloading' && dl.eta && (
                  <p className="text-body-sm">{dl.eta}</p>
                )}
                <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                  {dl.status === 'paused' && <button className="btn-text">Resume</button>}
                  {dl.status === 'downloading' && <button className="btn-text">Pause</button>}
                  {dl.status === 'waiting_wifi' && <button className="btn-text">Resume</button>}
                  {dl.status === 'completed' && <button className="btn-text">Open folder</button>}
                  <button className="btn-text">More</button>
                </div>
              </div>
            </GlassPanel>
          )
        })}
        {downloads.length === 0 && (
          <MessageState heading="No downloads yet." message="Find a book and choose Download to save it for your favorite listening app." action="Browse audiobooks" onAction={() => navigate('/')} />
        )}
      </div>
    </div>
  )
}
