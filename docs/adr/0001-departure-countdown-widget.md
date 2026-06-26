# Departure Countdown Widget

The Android home-screen widget will use Jetpack Glance to render a compact departure countdown for the same effective route used by the app, including enabled time-of-day routes. The widget will derive state through a widget-specific domain/use-case layer over the existing settings and journeys APIs, extend the journey window with the stored next cursor before forcing a refresh, and degrade gracefully when Android throttles minute-level updates.

## Considered Options

- Reuse the existing Compose `DepartureTimeCard` directly: rejected because Android app widgets cannot host arbitrary Compose UI.
- Build the widget with `RemoteViews`: rejected because Jetpack Glance better matches the app's Compose-first UI style while still compiling to app-widget-compatible views.
- Give the widget a separate route picker: deferred because the first widget should follow the app's effective route and avoid duplicating settings behavior.

## Consequences

The widget gets its own `DepartureWidgetState` instead of reusing `DepartureTimeCardData`, because loading, empty, refresh-failed, active countdown, and secondary-departure states are widget-specific. Tapping the widget opens Home rather than a trip detail screen so the destination cannot become stale when the soonest journey rolls over.
