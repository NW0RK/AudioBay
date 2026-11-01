import { categories, audiobooks } from '../mockData'
import { AudiobookCard } from '../components/AudiobookCard'
import { Icon, MessageState } from '../components/Shared'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export const CategoriesScreen = () => {
  const [selected, setSelected] = useState<string | null>(null)
  const navigate = useNavigate()
  const books = selected ? audiobooks.filter(b => b.category === categories.find(c => c.slug === selected)?.displayName) : []

  return (
    <div className="page">
      <div style={{ display: 'flex', alignItems: 'center', padding: '8px 16px', position: 'sticky', top: 0, background: 'var(--surface)', zIndex: 10 }}>
        <button onClick={() => selected ? setSelected(null) : navigate(-1)} className="icon-btn">
          <Icon name="back" size={24} />
        </button>
        <h1 className="text-title-md" style={{ marginLeft: '8px' }}>
          {selected ? categories.find(c => c.slug === selected)?.displayName : 'Categories'}
        </h1>
      </div>
      <div style={{ padding: '16px' }}>
        {!selected ? (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            {categories.map((cat) => (
              <button key={cat.slug} className="btn-tonal" onClick={() => setSelected(cat.slug)}>{cat.displayName}</button>
            ))}
          </div>
        ) : books.length === 0 ? (
          <MessageState heading="No audiobooks found." message="Try another category to find your next book." action="Categories" onAction={() => setSelected(null)} />
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(156px, 1fr))', gap: '12px 12px 16px' }}>
            {books.map((book) => (
              <AudiobookCard key={book.id} book={book} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
