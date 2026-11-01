import { useParams, useNavigate } from 'react-router-dom'
import { audiobooks } from '../mockData'
import { Icon, GlassPanel } from '../components/Shared'
import { useState } from 'react'

export const DetailScreen = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const book = audiobooks.find(b => b.id === id)!
  const [expanded, setExpanded] = useState(false)

  if (!book) return <div style={{ padding: '40px', textAlign: 'center' }}>Book not found</div>

  return (
    <div className="page" style={{ position: 'relative' }}>
      <div style={{
        position: 'absolute', top: '8px', left: '16px', width: '44px', height: '44px',
        background: 'rgba(25,27,28,0.78)', border: '1px solid rgba(232,226,216,0.22)', borderRadius: '12px',
        display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 10, cursor: 'pointer',
      }} onClick={() => navigate(-1)}>
        <Icon name="back" size={24} />
      </div>
      <div style={{
        height: '260px', background: 'var(--surface-container)', display: 'flex',
        alignItems: 'center', justifyContent: 'center', position: 'relative', overflow: 'hidden',
      }}>
        <img src={book.coverUrl} alt="" style={{ maxHeight: '100%', objectFit: 'contain', padding: '12px 64px' }} />
        {book.seeders && (
          <button onClick={() => navigate('/downloads')} style={{
            position: 'absolute', top: '12px', right: '12px', width: '48px', height: '48px',
            borderRadius: '50%', background: 'var(--primary)', color: 'var(--on-primary)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Icon name="download" size={24} />
          </button>
        )}
      </div>
      <div style={{ padding: '0 16px' }}>
        <div style={{ textAlign: 'center', marginTop: '20px' }}>
          <h1 className="text-headline-md" style={{ marginBottom: '8px' }}>{book.title}</h1>
          <p className="text-body-md" style={{ color: 'var(--on-surface-variant)', fontSize: '1.125rem' }}>{book.author}</p>
        </div>
        <GlassPanel className="" >
          <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {book.narrator && (
              <>
                <p className="text-body-md" style={{ whiteSpace: 'pre-wrap' }}>Narrated by{'\n'}{book.narrator}</p>
                <div style={{ height: '1px', background: 'var(--outline-variant)' }} />
              </>
            )}
            <span className="text-label-md" style={{ color: 'var(--primary)' }}>{book.source}</span>
            <p className="text-body-md">Language: {book.language}</p>
            <p className="text-body-md">Posted: {book.datePosted}</p>
            <p className="text-body-md">Shared by: {book.uploader}</p>
            <p className="text-body-md">Format: {book.format}</p>
            <p className="text-body-md">Bitrate: {book.bitrate}</p>
            <p className="text-body-md">Size: {book.size}</p>
          </div>
        </GlassPanel>
        <div style={{ marginTop: '24px' }}>
          <GlassPanel>
            <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <h2 className="text-title-md" style={{ fontSize: '1.375rem' }}>About this book</h2>
              <p className="text-body-md" style={{
                display: '-webkit-box',
                WebkitLineClamp: expanded ? 'unset' : 7,
                WebkitBoxOrient: 'vertical',
                overflow: 'hidden',
              }}>{book.description}</p>
              <button className="btn-text" onClick={() => setExpanded(!expanded)}>{expanded ? 'Show less' : 'Read more'}</button>
            </div>
          </GlassPanel>
        </div>
        <div style={{ marginTop: '24px' }}>
          <GlassPanel>
            <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <h2 className="text-title-md" style={{ fontSize: '1.375rem' }}>Torrent information</h2>
              <p className="text-body-sm" style={{ wordBreak: 'break-all' }}>a8f3c2e1b9d4765f0c2a1b3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3</p>
              <p className="text-body-sm">Seeders: {book.seeders} · Leechers: {book.leechers}</p>
            </div>
          </GlassPanel>
        </div>
        <div style={{ marginTop: '24px' }}>
          <h2 className="text-title-md" style={{ fontSize: '1.375rem', marginBottom: '12px' }}>Comments (3)</h2>
          <button className="btn-text" style={{ marginBottom: '12px' }}>Refresh comments</button>
          {['Great narration, really enjoyed this one!', 'The bitrate could be better but the story is amazing.', 'Thanks for sharing this!'].map((text, i) => (
            <GlassPanel key={i}>
              <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <span className="text-label-md" style={{ color: 'var(--primary)' }}>{i === 0 ? 'bookworm42' : 'Reader'}</span>
                <span className="text-body-sm" style={{ color: 'var(--on-surface-variant)' }}>2026-10-{20 + i}</span>
                <p className="text-body-md">{text}</p>
              </div>
            </GlassPanel>
          ))}
          <div style={{ marginTop: '24px' }}>
            <h2 className="text-title-md" style={{ fontSize: '1.375rem' }}>Chapters (18)</h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', padding: '8px 0 24px' }}>
              {Array.from({ length: 8 }).map((_, i) => (
                <p key={i} className="text-body-md" style={{ padding: '4px 0' }}>{i + 1}. Chapter {i + 1}</p>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
