import { audiobooks, categories } from '../mockData'
import { FeaturedHero, AudiobookCard } from '../components/AudiobookCard'
import { SectionHeading } from '../components/Shared'
import { useNavigate } from 'react-router-dom'

export const HomeScreen = () => {
  const navigate = useNavigate()
  return (
    <div className="page">
      <div style={{ padding: '20px 24px' }}>
        <h1 className="text-headline-lg">AudioBookBay</h1>
        <p style={{ color: 'var(--on-surface-variant)', fontSize: '0.875rem' }}>Discover</p>
      </div>
      <div style={{ padding: '0 16px' }}>
        <SectionHeading text="Discover something new" />
        <div style={{ display: 'flex', gap: '16px', overflowX: 'auto', padding: '4px 0 12px', scrollSnapType: 'x mandatory' }}>
          {audiobooks.slice(0, 6).map((book) => (
            <FeaturedHero key={book.id} book={book} />
          ))}
        </div>
        <SectionHeading text="Recently added" />
        <div style={{ display: 'flex', gap: '12px', overflowX: 'auto', paddingBottom: '12px' }}>
          {audiobooks.slice(0, 12).map((book) => (
            <div key={book.id} style={{ width: '300px', flexShrink: 0 }}>
              <AudiobookCard book={book} compact />
            </div>
          ))}
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <SectionHeading text="Categories" />
          <button className="btn-text" onClick={() => navigate('/categories')}>Browse all</button>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', paddingBottom: '12px' }}>
          {categories.slice(0, 6).map((cat) => (
            <button key={cat.slug} className="btn-tonal" onClick={() => navigate('/categories')}>{cat.displayName}</button>
          ))}
        </div>
        <SectionHeading text="Explore the catalog" />
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {audiobooks.slice(2, 8).map((book) => (
            <AudiobookCard key={book.id} book={book} compact />
          ))}
        </div>
      </div>
    </div>
  )
}
