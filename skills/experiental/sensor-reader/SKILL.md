---
name: sensor-reader
description: Reads available phone sensor data and returns a structured snapshot.
---

# Sensor Reader

This skill reads available device sensor data when running on a phone-like environment.

## Examples

* "Read my phone sensors"
* "Get accelerometer and orientation data"
* "Get sensor snapshot in 4 seconds"

## Instructions

Call the `run_js` tool with the following exact parameters:

- script name: `index.html`
- data: A JSON string with the following optional fields:
  - sensors: Array of strings. Requested sensors to read. Supported values: `accelerometer`, `gyroscope`, `orientation`, `geolocation`.
  - timeout_ms: Number. Max wait time for a sensor snapshot. Default: `3500`.
  - include_raw: Boolean. Whether to include full raw values in the response. Default: `true`.

If the user does not specify sensors, use:

```json
{
  "sensors": ["accelerometer", "gyroscope", "orientation"],
  "timeout_ms": 3500,
  "include_raw": true
}
```

After tool execution:

1. Summarize what data was captured.
2. Clearly mention unsupported or permission-blocked sensors.
3. If no sensors were captured, explain likely reasons (not a phone, permission denied, unavailable API).
