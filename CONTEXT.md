# Gautimes

Gautimes helps riders find upcoming Gautrain journeys between selected stations.

## Language

**Effective Route**:
The currently applicable journey route, expressed as a from station and a to station, after applying the user's default settings and any enabled time-of-day route rules.
_Avoid_: Widget route, home route, current defaults

**Time-of-Day Route**:
A configured morning or afternoon route that can replace the default route during its active part of the day when scheduled defaults are enabled.
_Avoid_: Schedule time, routine, automatic defaults

**Journey Window**:
The currently cached set of journeys for an effective route, ordered by departure time and extendable by requesting the next page of journeys.
_Avoid_: Batch, schedule set, cached times

**Departure Countdown**:
A glanceable display of the minutes remaining until the soonest upcoming departure for the effective route.
_Avoid_: Timer, progress card, next train card

**Secondary Departures**:
The next few upcoming departure times after the soonest departure, shown only when there is enough widget space.
_Avoid_: Schedule list, extra times

**Widget Empty State**:
A glanceable state shown when no upcoming journeys can be found or refreshed for the effective route.
_Avoid_: Error card, expired countdown
