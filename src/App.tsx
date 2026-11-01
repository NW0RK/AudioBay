import { Routes, Route } from 'react-router-dom'
import { FloatingNav } from './components/FloatingNav'
import { HomeScreen } from './screens/HomeScreen'
import { SearchScreen } from './screens/SearchScreen'
import { CategoriesScreen } from './screens/CategoriesScreen'
import { DetailScreen } from './screens/DetailScreen'
import { DownloadsScreen } from './screens/DownloadsScreen'
import { SavedScreen } from './screens/SavedScreen'
import { SettingsScreen } from './screens/SettingsScreen'

export default function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<HomeScreen />} />
        <Route path="/search" element={<SearchScreen />} />
        <Route path="/categories" element={<CategoriesScreen />} />
        <Route path="/detail/:id" element={<DetailScreen />} />
        <Route path="/downloads" element={<DownloadsScreen />} />
        <Route path="/saved" element={<SavedScreen />} />
        <Route path="/settings" element={<SettingsScreen />} />
      </Routes>
      <FloatingNav />
    </>
  )
}
