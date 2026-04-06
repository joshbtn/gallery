---
name: commute-pulse
description: Recommends the best departure windows using local weather and nearby transit context.
---

# Commute Pulse

Use this skill to suggest practical departure windows for short commutes.

## Examples

* "When should I leave in the next hour?"
* "Find the best commute window from my location"
* "Plan my walk with weather-aware timing"

## Instructions

Call the `run_js` tool with the following exact parameters:

- script name: `index.html`
- data: A JSON string with these fields:
  - latitude: Number. Current latitude.
  - longitude: Number. Current longitude.
  - destination: Optional object:
    - latitude: Number. Destination latitude.
    - longitude: Number. Destination longitude.
    - label: Optional string.
  - mode: Optional string. `transit`, `walk`, `bike`, or `drive`. Default: `transit`.
  - depart_within_minutes: Optional number. Search horizon. Default: `90`.

If location is not provided, ask the user for location before running.

Default payload:

```json
{
  "latitude": 37.7749,
  "longitude": -122.4194,
  "mode": "transit",
  "depart_within_minutes": 90
}
```

After tool execution:

1. Provide top 3 departure windows with reasons.
2. Mention expected weather friction and nearby transit stop availability.
3. If destination is given, include rough travel time range.
