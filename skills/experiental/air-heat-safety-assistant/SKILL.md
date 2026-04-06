---
name: air-heat-safety-assistant
description: Monitors hourly air quality, UV, and heat risk to suggest safer outdoor time windows.
---

# Air and Heat Safety Assistant

Use this skill to identify safer times for outdoor activity in the next hours.

## Examples

* "Is it safe to run outside this afternoon?"
* "Give me the safest 2-hour window today"
* "Check air and heat risk near me"

## Instructions

Call the `run_js` tool with the following exact parameters:

- script name: `index.html`
- data: A JSON string with these fields:
  - latitude: Number. Current latitude.
  - longitude: Number. Current longitude.
  - hours_ahead: Optional number. Forecast window in hours (max 24). Default: `12`.
  - activity_level: Optional string. `light`, `moderate`, `intense`. Default: `moderate`.
  - sensitivity: Optional string. `normal` or `sensitive`. Default: `normal`.

If location is not provided, ask the user for location before running.

Default payload:

```json
{
  "latitude": 37.7749,
  "longitude": -122.4194,
  "hours_ahead": 12,
  "activity_level": "moderate",
  "sensitivity": "normal"
}
```

After tool execution:

1. Provide the safest and riskiest windows.
2. Explain which factor drove risk (heat, AQ, UV, or combined).
3. Give practical precautions tailored to activity level and sensitivity.
