import { useState } from 'react'
import { audiobooks, searchHistory } from '../mockData'
import { AudiobookCard } from '../components/AudiobookCard'
import { Icon, MessageState, SectionHeading } from '../components/Shared'
import { useNavigate } from 'react-router-dom'

export const SearchScreen = () => {
  const [query, setQuery] = useState('')
  const navigate = useNavigate()
  const results = query ? audiobooks.filter(b => b.title.toLowerCase().includes(query.toLowerCase()) || b.author.toLowerCase().includes(query.toLowerCase())) : []

  return (
    <div className="page">
      <div style={{ padding: '16px' }}>
        <h1 className="text-headline-lg" style={{ margin: '16px 0' }}>Search</h1>
        <p style={{ color: 'var(--on-surface-variant)', fontSize: '0.875rem', marginBottom: '16px' }}>Find your next download by title, author, or series.</p>
        <div style={{ position: 'relative', marginBottom: '16px' }}>
          <div style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--on-surface-variant)' }}>
            <Icon name="search" size={20} />
          </div>
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search audiobook titles"
            style={{
              width: '100%', padding: '16px 48px', borderRadius: '28px', fontSize: '0.875rem',
              background: 'var(--surface-container)', color: 'var(--on-surface)',
              border: '1px solid var(--outline-variant)', outline: 'none',
            }}
          />
          {query && (
            <button onClick={() => setQuery('')} style={{ position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)' }}>
              <Icon name="close" size={20} />
            </button>
          )}
        </div>
        {query && results.length > 0 && (
          <p className="text-label-md" style={{ color: 'var(--primary)', margin: '16px 0' }}>{results.length} books found so far</p>
        )}
        {!query && searchHistory.length > 0 && (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <SectionHeading text="Recent searches" />
              <button className="btn-text">Clear history</button>
            </div>
            <div style={{ display: 'flex', gap: '8px', overflowX: 'auto' }}>
              {searchHistory.map((item) => (
                <button key={item} onClick={() => setQuery(item)} className="btn-tonal" style={{ minHeight: '36px', borderRadius: '16px' }}>{item}</button>
              ))}
            </div>
          </div>
        )}
        {!query && (
          <MessageState heading="Search for your next audiobook" message="Find your next download by title, author, or series." action="Browse audiobooks" onAction={() => navigate('/')} />
        )}
        {query && results.length === 0 && (
          <MessageState heading="No matching audiobooks found." message="Try a different title or author, or clear your search." action="Clear search" onAction={() => setQuery('')} />
        )}
        {query && results.length > 0 && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: '16px', paddingTop: '16px' }}>
            {results.map((book) => (
              <AudiobookCard key={book.id} book={book} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
