# Pressmark – Agent Instructions

You are working in the Pressmark Android app (Kotlin, Jetpack Compose, Room).

## Architecture
- feature/ = screen-owned UI, ViewModels, components
- core/ = shared UI + utilities
- data/ = Room entities, DAOs, repositories
- app/ = navigation and wiring only

## Rules
- Prefer small, focused edits
- Do NOT change Gradle files unless explicitly asked
- Do NOT change Room schema unless explicitly asked
- Database wipes are acceptable during early development
- Keep code professional and readable
- Follow existing patterns and naming

## Workflow
- Make a plan first
- List files you will touch
- Apply minimal diffs
- Summarize what changed and how to verify

## UI
- Use Material3
- Keep layouts simple and consistent
- Avoid unnecessary recomposition
