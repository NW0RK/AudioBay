# Personalized discovery

Home generates recommendations only when Generate picks is pressed. A completed, verified shared-storage publication and a personal Gemini API key are required. Opening Home and refreshing the catalog do not request AI recommendations.

Each generation sends up to 30 distinct recent completed book titles and authors to Gemini using the existing encrypted personal key. Gemini proposes four search queries, and then chooses at most six IDs from actual catalog results. Generated URLs and invented catalog identities are never used. Completed books, current queue items, duplicate title/author pairs, and the previous batch are excluded. Empty results and failures retain the last successful batch; that batch is available offline. Requests have a 60-second overall deadline and no automatic retry. Catalog requests do not modify interactive search history or pagination state.

Room migration 4 to 5 retains legacy tables and backfills valid completed, published queue records. Subsequent verified publications record history before their completion checkpoint. Removing queue rows does not remove this history or shared audio. Clear recommendation history cancels active generation and atomically deletes history and cached picks. Restoring completed downloads does not repopulate cleared history.

Validation uses deterministic protocol, ViewModel and device UI fixtures. Real paid Gemini generation is not invoked by automated checks; provider availability, key permissions, quota and catalog coverage can affect live results. Public builds contain no developer API key.
