import { Icon, GlassPanel, SectionHeading } from '../components/Shared'
import { useState } from 'react'

export const SettingsScreen = () => {
  const [wifiOnly, setWifiOnly] = useState(true)
  const [keySaved, setKeySaved] = useState(false)
  const [keyInput, setKeyInput] = useState('')
  const [checking, setChecking] = useState(false)
  const [updateStatus, setUpdateStatus] = useState<string | null>(null)

  return (
    <div className="page">
      <div style={{ padding: '16px' }}>
        <h1 className="text-headline-lg" style={{ margin: '16px 0' }}>Settings</h1>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <div>
            <SectionHeading text="Download location" />
            <GlassPanel>
              <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span className="text-body-md">Audiobook folder</span>
                  <button className="btn-text">Change</button>
                </div>
                <p className="text-body-sm">Author / Book Title / audio files</p>
                <p className="text-body-sm">Completed shared files stay on your phone when you remove a download or uninstall this app.</p>
              </div>
            </GlassPanel>
          </div>
          <div>
            <SectionHeading text="Network" />
            <GlassPanel>
              <div style={{ padding: '16px', display: 'flex', alignItems: 'center', gap: '12px' }}>
                <Icon name="wifiOff" size={24} />
                <div style={{ flex: 1 }}>
                  <p className="text-title-md">Download over Wi-Fi only</p>
                  <p className="text-body-sm">Transfers pause on mobile data and resume automatically when Wi-Fi returns.</p>
                </div>
                <div onClick={() => setWifiOnly(!wifiOnly)} style={{ width: '44px', height: '24px', borderRadius: '12px', background: wifiOnly ? 'var(--primary)' : 'var(--surface-variant)', position: 'relative', cursor: 'pointer' }}>
                  <div style={{ position: 'absolute', left: wifiOnly ? '22px' : '2px', top: '2px', width: '20px', height: '20px', borderRadius: '50%', background: 'var(--on-primary)', transition: 'left 0.2s' }} />
                </div>
              </div>
            </GlassPanel>
          </div>
          <div>
            <SectionHeading text="AI name correction" />
            <GlassPanel>
              <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <p className="text-body-sm">Add your Gemini API key to clean book and author names. It is encrypted on this device and excluded from backups. Using correction sends the entered names to Google and uses your API quota.</p>
                <div>
                  <label className="text-body-sm" style={{ display: 'block', marginBottom: '4px' }}>Gemini API key</label>
                  <input type="password" value={keyInput} onChange={(e) => setKeyInput(e.target.value)} placeholder="Paste your API key" style={{ width: '100%', padding: '12px', borderRadius: '12px', background: 'var(--surface-container)', color: 'var(--on-surface)', border: '1px solid var(--outline-variant)', outline: 'none' }} />
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button className="btn-filled" onClick={() => setKeySaved(true)}>Save key</button>
                  {keySaved && <button className="btn-text">Remove key</button>}
                </div>
                {keySaved && <p className="text-body-sm" style={{ color: 'var(--success-fg)' }}>Your key is saved on this device.</p>}
              </div>
            </GlassPanel>
          </div>
          <div>
            <SectionHeading text="App updates" />
            <GlassPanel>
              <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <p className="text-body-sm">Checks GitHub when opened, at most once daily. Updates download only when you choose.</p>
                <p className="text-body-sm">Installed version: 0.1.2</p>
                <button className="btn-tonal" onClick={() => { setChecking(true); setTimeout(() => { setChecking(false); setUpdateStatus('You have the latest compatible version.') }, 1500) }}>
                  {checking ? 'Checking...' : 'Check for updates'}
                </button>
                {updateStatus && <p className="text-body-sm" style={{ color: 'var(--success-fg)' }}>{updateStatus}</p>}
              </div>
            </GlassPanel>
          </div>
          <div>
            <SectionHeading text="Crash reports" />
            <GlassPanel>
              <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <p className="text-body-sm">Reports stay on this phone until you choose to share them. After a crash, reopen the app and visit Settings. Report to GitHub opens an issue draft for you to review and submit; GitHub issues are public.</p>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button className="btn-text">View report 1</button>
                  <button className="btn-text">Copy reports</button>
                  <button className="btn-text">Delete saved reports</button>
                </div>
              </div>
            </GlassPanel>
          </div>
        </div>
      </div>
    </div>
  )
}
