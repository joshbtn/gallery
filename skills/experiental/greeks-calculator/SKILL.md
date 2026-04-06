---
name: greeks-calculator
description: Computes Black-Scholes option Greeks and quick what-if scenarios for calls and puts.
---

# Greeks Calculator

Use this skill to calculate option Greeks and run simple scenario checks.

## Examples

* "Calculate Greeks for a call option"
* "Show Delta and Theta for this put"
* "Run what-if scenarios for spot up 2% and IV down 3 points"

## Instructions

Call the `run_js` tool with the following exact parameters:

- script name: `index.html`
- data: A JSON string with these fields:
  - option_type: String. `call` or `put`.
  - spot: Number. Current underlying price.
  - strike: Number. Option strike price.
  - volatility: Number. Annual implied volatility as decimal (for example `0.25` for 25%).
  - rate: Number. Annual risk-free rate as decimal (for example `0.04` for 4%).
  - time_to_expiry_days: Number. Days to expiration.
  - dividend_yield: Optional number. Annual dividend yield as decimal. Default: `0`.
  - scenarios: Optional array of objects for what-if checks:
    - spot_pct: Optional number. Percent move in underlying (for example `2` for +2%).
    - vol_pct: Optional number. Percentage-point move in volatility (for example `-3` means -3 points).
    - days_forward: Optional number. Days elapsed in scenario.

If the user does not provide inputs, use:

```json
{
  "option_type": "call",
  "spot": 100,
  "strike": 100,
  "volatility": 0.25,
  "rate": 0.04,
  "time_to_expiry_days": 30,
  "dividend_yield": 0,
  "scenarios": [
    { "spot_pct": 2, "vol_pct": -3, "days_forward": 1 },
    { "spot_pct": -2, "vol_pct": 3, "days_forward": 1 }
  ]
}
```

After tool execution:

1. Summarize key Greeks in plain language.
2. Highlight the scenario with the largest option value change.
3. Mention this is educational math output, not financial advice.
