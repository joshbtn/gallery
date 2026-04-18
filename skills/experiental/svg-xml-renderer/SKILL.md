---
name: svg-xml-renderer
description: Renders user-provided SVG XML and returns it as an image preview.
---

# SVG XML Renderer

Use this skill when the user wants to render, preview, or validate SVG markup.

## Instructions

You MUST call the `run_js` tool with the following exact parameters:

- script name: `index.html`
- data: A JSON string with the following field:
  - svgXml: String. The full SVG XML content to render.

If the user did not provide SVG XML, ask them to paste the full SVG XML first.

After tool execution:
1. Tell the user whether rendering succeeded.
2. If rendering failed, show the returned error and ask for corrected SVG XML.
