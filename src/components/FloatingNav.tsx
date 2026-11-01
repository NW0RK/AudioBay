import { NavLink } from 'react-router-dom'
import { Icon } from './Shared'
import { ReactNode } from 'react'

const items = [
  { label: 'Home', icon: 'home', to: '/' },
  { label: 'Search', icon: 'search', to: '/search' },
  { label: 'Downloads', icon: 'download', to: '/downloads' },
  { label: 'Saved', icon: 'heartOutline', to: '/saved' },
  { label: 'Settings', icon: 'settings', to: '/settings' },
]

export const FloatingNav = () => (
  <div style={{ position: 'fixed', bottom: 0, left: '50%', transform: 'translateX(-50%)', width: '100%', maxWidth: '440px', padding: '8px 16px 20px', zIndex: 100, pointerEvents: 'none' }}>
    <div className="glass-panel" style={{ display: 'flex', borderRadius: '999px', height: '64px', padding: '0 8px', pointerEvents: 'auto', maxWidth: '400px', margin: '0 auto' }}>
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === '/'}
          style={{ textDecoration: 'none' }}
        >
          {({ isActive }) => (
            <div style={{
              flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%',
              position: 'relative', borderRadius: '50%',
            }}>
              {isActive && (
                <div style={{ position: 'absolute', width: '44px', height: '44px', borderRadius: '50%', background: 'var(--primary)', opacity: 0.18, transition: 'opacity 0.2s' }} />
              )}
              <Icon name={item.icon} size={24} className="" />
            </div>
          )}
        </NavLink>
      ))}
    </div>
  </div>
)
