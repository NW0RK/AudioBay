export interface Audiobook {
  id: string
  title: string
  author: string
  narrator?: string
  coverUrl: string
  category?: string
  duration?: string
  size?: string
  language?: string
  format?: string
  bitrate?: string
  datePosted?: string
  uploader?: string
  description?: string
  source: 'AudioBookBay' | 'LibriVox'
  seeders?: number
  leechers?: number
}

export interface DownloadItem {
  id: string
  title: string
  author: string
  status: 'queued' | 'checking' | 'downloading' | 'paused' | 'waiting_wifi' | 'publishing' | 'completed' | 'error'
  progress: number
  speed?: string
  peers?: number
  eta?: string
  size?: string
}

export interface Category {
  slug: string
  displayName: string
}

const covers = [
  'https://images.pexels.com/photos/35405332/pexels-photo-35405332.jpeg?auto=compress&cs=tinysrgb&h=650&w=500',
  'https://images.pexels.com/photos/17849670/pexels-photo-17849670.jpeg?auto=compress&cs=tinysrgb&h=650&w=500',
  'https://images.pexels.com/photos/7265376/pexels-photo-7265376.jpeg?auto=compress&cs=tinysrgb&h=650&w=500',
  'https://images.pexels.com/photos/7451970/pexels-photo-7451970.jpeg?auto=compress&cs=tinysrgb&h=650&w=500',
  'https://images.pexels.com/photos/5799506/pexels-photo-5799506.jpeg?auto=compress&cs=tinysrgb&h=650&w=500',
  'https://images.pexels.com/photos/17028571/pexels-photo-17028571.jpeg?auto=compress&cs=tinysrgb&h=650&w=500',
  'https://images.pexels.com/photos/34746968/pexels-photo-34746968.jpeg?auto=compress&cs=tinysrgb&h=650&w=500',
  'https://images.pexels.com/photos/261956/pexels-photo-261956.jpeg?auto=compress&cs=tinysrgb&h=650&w=500',
]

const titles = [
  'The Midnight Library',
  'Where the Crawdads Sing',
  'The Seven Husbands of Evelyn Hugo',
  'Project Hail Mary',
  'The Thursday Murder Club',
  'Klara and the Sun',
  'The Invisible Life of Addie LaRue',
  'Pachinko',
  'The House in the Cerulean Sea',
  'Lessons in Chemistry',
  'The Lincoln Highway',
  'Cloud Cuckoo Land',
  'The Atlas Six',
  'The Night Circus',
  'Mexican Gothic',
  'The Poppy War',
]

const authors = [
  'Matt Haig',
  'Delia Owens',
  'Taylor Jenkins Reid',
  'Andy Weir',
  'Richard Osman',
  'Kazuo Ishiguro',
  'V.E. Schwab',
  'Min Jin Lee',
  'TJ Klune',
  'Bonnie Garmus',
  'Amor Towles',
  'Anthony Doerr',
]

const narrators = [
  'Sarah Smith',
  'James Anderson',
  'Emma Thompson',
  'Michael Kramer',
  'Julia Whelan',
]

const descriptions = [
  'A sweeping tale of love, loss, and the extraordinary power of ordinary lives. Set against the backdrop of a changing world, this story follows unforgettable characters through moments that define them.',
  'Between life and death there is a library. When Nora Seed finds herself in the Midnight Library, she has a chance to make things right. Each book contains a different version of the life she could have lived.',
  'A dazzling novel of glamour, romance, and old Hollywood secrets. An aging starlet tells the story of her seven marriages and the great love of her life that nobody knew about.',
  'A lone astronaut must save humanity in this irresistible tale of friendship, problem-solving, and the indomitable human spirit. Science has never been this much fun.',
]

const cats = ['Fiction', 'Fantasy', 'Mystery', 'Science Fiction', 'Romance', 'History', 'Thriller', 'Biography', 'Horror', 'Adventure']

export const audiobooks: Audiobook[] = titles.map((title, i) => ({
  id: `book-${i}`,
  title,
  author: authors[i % authors.length],
  narrator: narrators[i % narrators.length],
  coverUrl: covers[i % covers.length],
  category: cats[i % cats.length],
  duration: `${8 + (i % 10)}h ${15 + (i % 45)}m`,
  size: `${300 + (i * 50)} MB`,
  language: i % 3 === 0 ? 'Spanish' : 'English',
  format: i % 2 === 0 ? 'MP3' : 'M4B',
  bitrate: i % 2 === 0 ? '64 kbps' : '128 kbps',
  datePosted: `2026-10-${(i % 28 + 1).toString().padStart(2, '0')}`,
  uploader: i % 2 === 0 ? 'bookworm42' : 'audiophile',
  description: descriptions[i % descriptions.length],
  source: i % 4 === 0 ? 'LibriVox' : 'AudioBookBay',
  seeders: 12 + (i * 7) % 80,
  leechers: 3 + (i * 3) % 20,
}))

export const categories: Category[] = [
  { slug: 'fiction', displayName: 'Fiction' },
  { slug: 'fantasy', displayName: 'Fantasy' },
  { slug: 'mystery', displayName: 'Mystery' },
  { slug: 'scifi', displayName: 'Science Fiction' },
  { slug: 'romance', displayName: 'Romance' },
  { slug: 'history', displayName: 'History' },
  { slug: 'thriller', displayName: 'Thriller' },
  { slug: 'biography', displayName: 'Biography' },
]

export const downloads: DownloadItem[] = [
  { id: 'd0', title: 'The Midnight Library', author: 'Matt Haig', status: 'downloading', progress: 0.42, speed: '124.3', peers: 7, eta: '12m left', size: '320 MB' },
  { id: 'd1', title: 'Where the Crawdads Sing', author: 'Delia Owens', status: 'completed', progress: 1, size: '450 MB' },
  { id: 'd2', title: 'Project Hail Mary', author: 'Andy Weir', status: 'paused', progress: 0.68, size: '580 MB' },
  { id: 'd3', title: 'The Thursday Murder Club', author: 'Richard Osman', status: 'waiting_wifi', progress: 0.15, size: '310 MB' },
]

export const savedBooks = audiobooks.slice(4, 10)

export const searchHistory = ['Andy Weir', 'Midnight Library', 'Taylor Jenkins Reid']
