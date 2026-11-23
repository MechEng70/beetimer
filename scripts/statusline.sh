#!/usr/bin/env bash
# Claude Code status line for Bee Timer rounds (playbook section 9).
# Reads the JSON Claude Code sends on stdin and prints: model, 5-hour %, weekly % with today's weekly budget,
# reset countdowns and the current task from docs/HANDOFF.md. Writes the same numbers to
# ~/.claude/beetimer-usage.json (outside the repo). Then prints the status line that was there before
# (the caveman badge), so that one keeps working.
set -u
PREV_STATUSLINE="$HOME/.claude/plugins/cache/caveman/caveman/63e797cd753b/hooks/caveman-statusline.sh"
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$HOME/.claude/beetimer-usage.json"

input="$(cat)"
now=$(date +%s)

# resets_at may be epoch seconds or an ISO time; normalize to epoch seconds (empty if missing).
to_epoch() {
  local v="$1"
  [ -z "$v" ] || [ "$v" = "null" ] && { echo ""; return; }
  if [[ "$v" =~ ^[0-9]+(\.[0-9]+)?$ ]]; then
    local i=${v%.*}
    # milliseconds if it's too big to be seconds
    [ "$i" -gt 100000000000 ] && i=$((i / 1000))
    echo "$i"
  else
    date -d "$v" +%s 2>/dev/null || echo ""
  fi
}

model=$(jq -r '.model.display_name // .model.id // "?"' <<<"$input" 2>/dev/null)
p5=$(jq -r '.rate_limits.five_hour.used_percentage // empty' <<<"$input" 2>/dev/null)
p7=$(jq -r '.rate_limits.seven_day.used_percentage // empty' <<<"$input" 2>/dev/null)
r5=$(to_epoch "$(jq -r '.rate_limits.five_hour.resets_at // empty' <<<"$input" 2>/dev/null)")
r7=$(to_epoch "$(jq -r '.rate_limits.seven_day.resets_at // empty' <<<"$input" 2>/dev/null)")

# Weekly budget: 20% per day of the weekly window, day N (1-based, from the last reset) gets 20*N, days 6 and 7 get 100.
budget=""
day=""
if [ -n "$r7" ]; then
  start=$((r7 - 7 * 86400))
  day=$(((now - start) / 86400 + 1))
  [ "$day" -lt 1 ] && day=1
  if [ "$day" -le 5 ]; then budget=$((day * 20)); else budget=100; fi
fi

task=$(grep -m1 -oE '^- \*\*Next:\*\* T-[0-9]+' "$REPO/docs/HANDOFF.md" 2>/dev/null | grep -oE 'T-[0-9]+')

left() { # seconds until epoch $1 as 2h05m or 3d04h
  [ -z "$1" ] && { echo "?"; return; }
  local s=$(($1 - now)); [ $s -lt 0 ] && s=0
  if [ $s -ge 86400 ]; then printf '%dd%02dh' $((s / 86400)) $((s % 86400 / 3600)); else printf '%dh%02dm' $((s / 3600)) $((s % 3600 / 60)); fi
}
fmt() { [ -z "$1" ] && echo "?" || printf '%.0f%%' "$1"; }

if [ -n "$p5$p7" ]; then
  jq -n --argjson ts "$now" --arg model "$model" \
    --arg p5 "$p5" --arg p7 "$p7" --arg r5 "$r5" --arg r7 "$r7" --arg budget "$budget" --arg day "$day" \
    --argjson raw "$(jq -c '.rate_limits // {}' <<<"$input" 2>/dev/null || echo '{}')" \
    '{updated: $ts, model: $model,
      five_hour: {used_percentage: ($p5 | tonumber? // null), resets_at: ($r5 | tonumber? // null)},
      seven_day: {used_percentage: ($p7 | tonumber? // null), resets_at: ($r7 | tonumber? // null),
                  day: ($day | tonumber? // null), budget_percentage: ($budget | tonumber? // null)},
      raw_rate_limits: $raw}' > "$OUT.tmp" 2>/dev/null && [ -s "$OUT.tmp" ] && mv "$OUT.tmp" "$OUT"
fi

line="$model | 5h $(fmt "$p5") (reset $(left "$r5")) | wk $(fmt "$p7")"
[ -n "$budget" ] && line="$line / budget ${budget}%"
line="$line (reset $(left "$r7"))"
[ -n "$task" ] && line="$line | $task"
printf '%s' "$line"

if [ -f "$PREV_STATUSLINE" ]; then
  prev=$(bash "$PREV_STATUSLINE" <<<"$input" 2>/dev/null)
  [ -n "$prev" ] && printf ' %s' "$prev"
fi
exit 0
