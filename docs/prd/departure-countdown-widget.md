# PRD: Departure Countdown Widget

Labels: `ready-for-agent`

## Problem Statement

As a Gautimes rider, I want a quick glanceable Android home-screen widget that tells me how many minutes remain until the soonest train departs for my effective route, so that I can check whether I need to leave without opening the app.

Today the app already shows a `DepartureTimeCard` on Home, and it already knows the user's default, morning, and afternoon station configurations. However, that information is only available after opening the app. The rider needs the same effective route and journey-window behavior surfaced as a compact widget that rolls over from one departure to the next.

## Solution

Add an Android home-screen widget built with Jetpack Glance. The widget shows a departure countdown for the same effective route used by the app, including enabled time-of-day routes. It displays the soonest upcoming train as the primary glanceable value, optionally shows secondary departures when space allows, rolls over through the current journey window, extends that window with the existing journeys API cursor when needed, and falls back to a forced refresh when paging cannot satisfy the widget.

Tapping the widget opens the app Home screen. The widget does not deep-link to a specific trip in this version because the soonest journey can change between render and tap.

## User Stories

1. As a Gautimes rider, I want to see the minutes until my next train on my Android home screen, so that I do not need to open the app for a quick departure check.
2. As a Gautimes rider, I want the widget to use my default from and to stations, so that it matches the route I already configured in the app.
3. As a Gautimes rider, I want the widget to respect my enabled morning route, so that it shows my commute route during the morning window.
4. As a Gautimes rider, I want the widget to respect my enabled afternoon route, so that it shows my return route during the afternoon window.
5. As a Gautimes rider, I want the widget to fall back to my default route outside configured time-of-day routes, so that it always has a sensible effective route.
6. As a Gautimes rider, I want the countdown to tick down by minute while a departure is active, so that the widget feels like a live countdown.
7. As a Gautimes rider, I want the widget to roll over to the next departure after the current one passes, so that it does not show an expired train.
8. As a Gautimes rider, I want the widget to fetch more journeys when the current journey window has no future departures, so that it continues showing useful times later in the day.
9. As a Gautimes rider, I want the widget to use the same journeys API as the app, so that widget and app data stay consistent.
10. As a Gautimes rider, I want the widget to reuse cached journeys where possible, so that it remains responsive and avoids unnecessary network calls.
11. As a Gautimes rider, I want the widget to refresh from the network when paging cannot provide a future departure, so that stale local data can recover.
12. As a Gautimes rider, I want the widget to show a clear empty state when no upcoming trains are available, so that I am not misled by an expired countdown.
13. As a Gautimes rider, I want the widget to show a clear refresh-failed state when data cannot be loaded, so that I know the issue is data availability rather than no service.
14. As a Gautimes rider, I want the widget to show the route label, so that I can verify which stations the countdown applies to.
15. As a Gautimes rider, I want the primary countdown to dominate the widget, so that it is readable at a glance.
16. As a Gautimes rider, I want secondary departures to appear only when the widget has enough space, so that the compact layout remains legible.
17. As a Gautimes rider, I want tapping the widget to open Home, so that I can inspect the full current journey list.
18. As a Gautimes rider, I want the widget to use the app's visual language, so that it feels like part of Gautimes.
19. As a Gautimes rider, I want the widget to handle Android background throttling gracefully, so that temporary lag does not produce an incorrect journey after refresh.
20. As a Gautimes rider, I want the widget to update when settings change, so that route changes are reflected without manual cache clearing.
21. As a Gautimes rider, I want the widget to initialize correctly when added to the launcher, so that it immediately becomes useful.
22. As a Gautimes rider, I want the widget to avoid showing reminder controls, so that the widget stays focused on the countdown.
23. As a Gautimes rider, I want the widget to remain useful in a compact launcher footprint, so that I can place it alongside other home-screen widgets.
24. As a developer, I want widget state to be derived through a testable use case, so that route selection, rollover, paging, refresh fallback, and countdown math can be verified without testing Glance internals.
25. As a developer, I want the widget renderer to consume a dedicated widget state model, so that widget-only loading, empty, error, active, and secondary-departure states are explicit.
26. As a developer, I want Glance code to stay thin, so that Android app-widget constraints do not leak into journey selection logic.
27. As a developer, I want the implementation to reuse existing repository and settings interfaces, so that the widget does not duplicate journey API behavior.
28. As a developer, I want the implementation to preserve the existing Home screen behavior, so that adding the widget does not regress the in-app departure card.

## Implementation Decisions

- The widget will be built with Jetpack Glance.
- The widget will follow the app's effective route: default stations plus enabled time-of-day routes for morning and afternoon.
- The widget will not introduce a separate per-widget route picker in this version.
- The widget will have a dedicated `DepartureWidgetState` model rather than reusing the in-app `DepartureTimeCardData`.
- The widget state model must represent loading, active countdown, widget empty state, refresh-failed state, route labels, the primary departure, and optional secondary departures.
- The widget will display the soonest upcoming departure as the primary content.
- Secondary departures may be shown as compact context only when the widget size allows.
- The widget will use the existing journeys API and repository behavior.
- When the cached journey window contains future departures, the widget uses that window.
- When the journey window contains no future departures and a next cursor is available, the widget requests the next journey window using the existing cursor-based load-more behavior.
- When no cursor is available or loading more cannot produce an upcoming journey, the widget forces a fresh journey query.
- The countdown should tick down per minute when Android allows it.
- Android app-widget periodic updates cannot be assumed to run every minute, so the widget must degrade gracefully under platform throttling.
- The widget must recompute the countdown from current time whenever it renders, rather than trusting stored countdown text.
- The widget must not keep showing an expired departure countdown.
- The widget tap target opens Home.
- The widget does not deep-link to Trip Details in this version.
- The widget should visually approximate the existing departure card, but arbitrary Compose UI reuse is not possible in Android app widgets.
- Widget-specific loading and scheduling work should be isolated from rendering.
- Existing domain and repository interfaces should be preferred over adding duplicate data access paths.
- No backend API changes are expected.
- No Room schema change is expected unless implementation discovers that widget scheduling metadata cannot be represented outside persistent storage.
- The architectural decision is recorded in ADR 0001.

## Testing Decisions

- The primary test seam is a new high-level widget state use case, such as `GetDepartureWidgetStateUseCase`.
- Tests should verify external behavior of the widget state, not implementation details of internal repository calls.
- The use-case tests should use fake settings, fake journey repository behavior, and a controllable clock.
- Test cases should cover default effective route selection.
- Test cases should cover morning time-of-day route selection when scheduled defaults are enabled.
- Test cases should cover afternoon time-of-day route selection when scheduled defaults are enabled.
- Test cases should cover fallback to default route when scheduled defaults are disabled.
- Test cases should cover fallback to default route when a time-of-day route is incomplete.
- Test cases should cover filtering expired journeys out of the journey window.
- Test cases should cover choosing the soonest upcoming departure.
- Test cases should cover countdown minute calculation from a fixed current time.
- Test cases should cover secondary departure selection and formatting.
- Test cases should cover loading more journeys when all cached journeys are expired and a next cursor exists.
- Test cases should cover forced refresh fallback when no next cursor exists.
- Test cases should cover forced refresh fallback when loading more fails.
- Test cases should cover widget empty state when no upcoming journeys are available after paging and refresh.
- Test cases should cover refresh-failed state when journey data cannot be loaded.
- Test cases should cover route-label output for the active effective route.
- Glance rendering should receive a smaller smoke test or manual verification because it should only render an already-decided state.
- Existing prior art includes unit tests for date/time utilities and DTO parsing; the new use-case tests should follow the same local unit-test style.

## Out of Scope

- Per-widget route configuration.
- Multiple independent widgets with different routes.
- Direct deep links to Trip Details.
- Reminder actions from the widget.
- A full schedule list inside the widget.
- Exact guaranteed minute-by-minute updates on every Android device despite platform background restrictions.
- Backend API changes.
- Replacing the in-app `DepartureTimeCard`.
- Reworking Home screen route selection beyond reuse needed by the widget.

## Further Notes

- The domain glossary defines effective route, time-of-day route, journey window, departure countdown, secondary departures, and widget empty state.
- ADR 0001 records the choice to use Jetpack Glance, a widget-specific state model, existing effective route behavior, journey-window rollover, and Home tap behavior.
- Android's normal app-widget periodic update interval should not be treated as sufficient for minute countdowns; the implementation needs explicit refresh scheduling with graceful degradation.
