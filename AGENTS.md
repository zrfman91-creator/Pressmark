# Pressmark – Agent Instructions

You are working in the Pressmark Android app (Kotlin, Jetpack Compose, Room).

## Architecture
- feature/ = screen-owned UI, ViewModels, components
- core/ = shared UI + utilities
- data/ = Room entities, DAOs, repositories
- app/ = navigation and wiring only

## Rules
- Prefer correctness and professionalism overall
- Large refactors are acceptable if they are defensible.
- Gradle file changes are acceptable with sound logic & explanation
- Room schema changes are acceptable with sound logic & explanation
- Database wipes are acceptable during early development
- Keep code professional and readable
- Follow existing patterns and naming
- Read the repository again if a follow up question/task is executed to avoid merge conflicts
- Keep package/file bloat to a minimum; Don't create a file to create a file; integrate into existing files if it is logical to do so
## Workflow
- Make a plan first, and explain why the changes are necessary and potential risk outcomes
- List files you will touch
- Summarize what changed and explain why those changes were made

## UI
- Use Material3
- Avoid changing existing UI components unless explicitly asked
- Avoid unnecessary recomposition
