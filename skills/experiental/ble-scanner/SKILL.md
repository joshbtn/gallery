---
name: ble-scanner
description: Scans for nearby Bluetooth Low Energy (BLE) devices and returns their names and identifiers, plus optional geolocation data.
---

# BLE Scanner

This skill uses the Web Bluetooth API to discover nearby Bluetooth Low Energy devices and optionally captures the device's geolocation at scan time.

## Examples

* "What Bluetooth devices are nearby?"
* "Scan for BLE devices close to me"
* "Find nearby Bluetooth devices and get my location"
* "List Bluetooth devices around me"
* "What BLE devices can I see right now?"

## Instructions

Call the `run_js` tool with the following exact parameters:

- script name: `index.html`
- data: A JSON string with the following optional fields:
  - include_location: Boolean. Whether to also capture geolocation at scan time. Default: `true`.
  - timeout_ms: Number. Max time in ms to wait for the geolocation fix. Default: `5000`.

Example payload:

```json
{
  "include_location": true,
  "timeout_ms": 5000
}
```

After tool execution:

1. List the discovered Bluetooth device names and IDs returned.
2. If geolocation was captured, report the latitude, longitude, and accuracy.
3. If the Web Bluetooth API is unsupported or permission was denied, explain that the device or browser may not support Web Bluetooth (requires Android Chrome or a compatible WebView with Bluetooth permission granted).
4. If no devices were found via `getDevices()` and the `requestDevice` picker was dismissed or timed out, tell the user no devices were selected and suggest they try again and pick a device from the system prompt.
5. Clearly mention any warnings about unsupported or unavailable capabilities.
