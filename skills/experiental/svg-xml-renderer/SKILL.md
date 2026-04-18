---
name: svg-xml-renderer
description: Renders user-provided SVG XML and returns it as an image preview.
---

# SVG XML Renderer

Use this skill when the user wants to render, preview, or validate SVG markup.

## Instructions

You MUST call the `run_js` tool with the following exact parameters:

- script name: `index.html`
- data: Prefer a JSON string with the following field:
  - svgXml: String. The full SVG XML content to render.
  - Ensure quotes are properly escaped if you use JSON.
  - Example: `{"svgXml":"<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\"></svg>"}`
  - If escaping becomes error-prone, you may pass raw SVG XML text directly as `data`.
  - The parser can recover embedded `<svg...></svg>` markup from malformed JSON when possible.

If the user did not provide SVG XML, ask them to paste the full SVG XML first.

After tool execution:
1. Tell the user whether rendering succeeded.
2. If rendering failed, show the returned error and ask for corrected SVG XML.
