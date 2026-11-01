import { Audiobook } from '../mockData'
import { Icon } from './Shared'
import { useNavigate } from 'react-router-dom'

export const AudiobookCard = ({ book, compact = false }: { book: Audiobook; compact?: boolean }) => {
  const navigate = useNavigate()
  return (
    <div className="glass-panel" style={{ cursor: 'pointer', overflow: 'hidden', transition: 'transform 0.14s, opacity 0.14s' }}
      onClick={() => navigate(`/detail/${book.id}`)}>
      {compact ? (
        <div style={{ display: 'flex', padding: '12px', gap: '12px' }}>
          <img src={book.coverUrl} alt="" style={{ width: '88px', height: '128px', borderRadius: '12px', objectFit: 'cover' }} />
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <span className="text-title-md" style={{ display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>{book.title}</span>
            <span className="text-body-sm">{book.author}</span>
            <span className="text-label-md" style={{ color: 'var(--primary)' }}>{[book.language, book.format, book.size].filter(Boolean).join(' · ')}</span>
            <div style={{ display: 'flex', marginTop: 'auto' }}>
              <div className="icon-btn"><Icon name="heartOutline" size={20} /></div>
              <div className="icon-btn"><Icon name="download" size={20} /></div>
            </div>
          </div>
        </div>
      ) : (
        <div>
          <div style={{ position: 'relative', aspectRatio: '0.75' }}>
            <img src={book.coverUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          </div>
          <div style={{ padding: '12px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <span className="text-title-md" style={{ display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>{book.title}</span>
            <span className="text-body-sm">{book.author}</span>
            <span className="text-label-md" style={{ color: 'var(--primary)' }}>{[book.language, book.format, book.size].filter(Boolean).join(' · ')}</span>
            <div style={{ display: 'flex' }}>
              <div className="icon-btn"><Icon name="heartOutline" size={20} /></div>
              <div className="icon-btn"><Icon name="download" size={20} /></div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export const FeaturedHero = ({ book }: { book: Audiobook }) => {
  const navigate = useNavigate()
  return (
    <div
      onClick={() => navigate(`/detail/${book.id}`)}
      style={{
        width: '300px', aspectRatio: '0.82', borderRadius: '20px', overflow: 'hidden', cursor: 'pointer',
        position: 'relative', flexShrink: 0, transition: 'transform 0.16s',
      }}
    >
      <img src={book.coverUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
      <div style={{
        position: 'absolute', inset: 0,
        background: 'linear-gradient(to bottom, transparent 45%, var(--surface) 100%)',
      }} />
      <div style={{ position: 'absolute', top: '8px', right: '8px', display: 'flex', gap: '4px' }}>
        <div className="icon-btn" style={{ width: '36px', height: '36px', background: 'rgba(25,27,28,0.4)', backdropFilter: 'blur(4px)' }}>
          <Icon name="heartOutline" size={20} />
        </div>
        <div className="icon-btn" style={{ width: '36px', height: '36px', background: 'rgba(25,27,28,0.4)', backdropFilter: 'blur(4px)' }}>
          <Icon name="download" size={20} />
        </div>
      </div>
      <div style={{ position: 'absolute', bottom: '16px', left: '16px', right: '16px' }}>
        <h3 style={{ fontSize: '1.375rem', fontWeight: 600, lineHeight: 1.2, marginBottom: '2px', color: 'var(--ink-9)', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>{book.title}</h3>
        <span style={{ fontSize: '0.875rem', color: 'var(--ink-7)', display: '-webkit-box', WebkitLineClamp: 1, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>{book.author}</span>
      </div>
    </div>
  )
}
