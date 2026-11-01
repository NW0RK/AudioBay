import { savedBooks } from '../mockData'
import { AudiobookCard } from '../components/AudiobookCard'
import { Icon, MessageState } from '../components/Shared'
import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

type SortKey = 'recent' | 'title' | 'author'

export const SavedScreen = () => {
  const navigate = useNavigate()
  const [grid, setGrid] = useState(true)
  const [sort, setSort] = useState<SortKey>('recent')
  const [sortMenu, setSortMenu] = useState(false)

  const books = (() => {
    const list = [...savedBooks]
    if (sort === 'title') list.sort((a, b) => a.title.localeCompare(b.title))
    if (sort === 'author') list.sort((a, b) => a.author.localeCompare(b.author) || a.title.localeCompare(b.title))
    return list
  })()

  useEffect(() => {
    const handler = () => setSortMenu(false)
    if (sortMenu) {
      window.addEventListener('click', handler)
      return () => window.removeEventListener('click', handler)
    }
  }, [sortMenu])

  return (
    <div className="page">
      <div style={{ padding: '16px' }}>
        <h1 className="text-headline-lg" style={{ margin: '16px 0' }}>Saved</h1>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', position: 'relative' }}>
          <div>
            <button className="btn-text" onClick={(e) => { e.stopPropagation(); setSortMenu(!sortMenu) }}>
              {sort === 'recent' ? 'Recently saved' : sort === 'title' ? 'Title' : 'Author'}
            </button>
            {sortMenu && (
              <div style={{ position: 'absolute', top: '32px', left: '0', background: 'var(--surface-container)', borderRadius: '12px', border: '1px solid var(--outline-variant)', zIndex: 20, padding: '4px' }}>
                {(['recent', 'title', 'author'] as SortKey[]).map((opt) => (
                  <button key={opt} onClick={(e) => { e.stopPropagation(); setSort(opt); setSortMenu(false) }} style={{ display: 'block', width: '100%', textAlign: 'left', padding: '10px 16px', borderRadius: '8px' }}>
                    {opt === 'recent' ? 'Recently saved' : opt === 'title' ? 'Title' : 'Author'}
                  </button>
                ))}
              </div>
            )}
          </div>
          <div style={{ display: 'flex', gap: '4px' }}>
            <button onClick={() => setGrid(true)} className="icon-btn" style={{ background: grid ? 'var(--secondary-container)' : 'transparent' }}>
              <Icon name="grid" size={20} />
            </button>
            <button onClick={() => setGrid(false)} className="icon-btn" style={{ background: !grid ? 'var(--secondary-container)' : 'transparent' }}>
              <Icon name="list" size={20} />
            </button>
          </div>
        </div>
        {books.length === 0 ? (
          <MessageState heading="Save books from discovery, search, or details to find them here later." message="Keep books here while you decide what to download next." action="Browse audiobooks" onAction={() => navigate('/')} />
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: grid ? 'repeat(auto-fill, minmax(156px, 1fr))' : '1fr', gap: '12px 12px 16px' }}>
            {books.map((book) => (
              <AudiobookCard key={book.id} book={book} compact={!grid} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
