<?php
// ============================================================
// icons.php — inline SVG line icons and KPI sparklines (no icon font,
// no extra requests). Icons use currentColor, so they follow the text.
// ============================================================

function icon($name, $size = 20) {
    static $paths = array(
        'home'     => 'M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z',
        'list'     => 'M9 6h12M9 12h12M9 18h12M4 6h.01M4 12h.01M4 18h.01',
        'calendar' => 'M5 5h14a1 1 0 0 1 1 1v13a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a1 1 0 0 1 1-1zM4 10h16M8 3v4M16 3v4',
        'database' => 'M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3-3.6 3-8 3-8-1.3-8-3zM4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3',
        'plus'     => 'M12 5v14M5 12h14',
        'back'     => 'M19 12H5M11 18l-6-6 6-6',
        'logout'   => 'M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3M10 17l5-5-5-5M15 12H3',
        'trash'    => 'M4 7h16M10 11v6M14 11v6M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V4h6v3',
        'upload'   => 'M12 15V4M7 9l5-5 5 5M4 15v4a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4',
        'download' => 'M12 4v11M7 10l5 5 5-5M4 15v4a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4',
        'expand'   => 'M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7',
        'close'    => 'M18 6 6 18M6 6l12 12',
        'table'    => 'M4 5h16v14H4zM4 10h16M4 15h16M10 5v14',
        'chart'    => 'M4 20h16M7 16v-5M12 16V6M17 16v-8',
        'chevron'  => 'M9 6l6 6-6 6',
        'alert'    => 'M12 9v4M12 17h.01M10.3 3.9 2.4 18a2 2 0 0 0 1.7 3h15.8a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z',
        'check'    => 'M5 12l5 5L20 7',
        'edit'     => 'M4 20h4L19 9l-4-4L4 16v4zM14 6l4 4',
        'leaf'     => 'M5 19c0-8 5-14 15-15-1 10-7 15-15 15zM5 19l7-7',
        'info'     => 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 16v-4M12 8h.01',
        'scale'    => 'M12 3v18M7 21h10M5 7h14M5 7l-3 7a3 3 0 0 0 6 0zM19 7l-3 7a3 3 0 0 0 6 0z',
    );
    $d = isset($paths[$name]) ? $paths[$name] : $paths['info'];
    return '<svg class="icon" width="' . (int)$size . '" height="' . (int)$size . '" viewBox="0 0 24 24" fill="none"'
        . ' stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'
        . '<path d="' . $d . '"/></svg>';
}

/**
 * Small trend line for a stat tile: past months in the muted tone, the
 * latest month as an accent dot. Nulls (no sales that month) are skipped.
 * $zeroBase anchors magnitudes at 0 (revenue, kg, count); rates such as
 * price use their own min so small changes stay visible.
 * The SVG stretches to its box (preserveAspectRatio none); strokes and the
 * dot use non-scaling strokes, so lines stay 2px and the dot stays round.
 * Returns '' when there are fewer than two points to draw.
 */
function sparkline($values, $zeroBase = true) {
    $pts = array();
    foreach (array_values($values) as $i => $v) {
        if ($v !== null) {
            $pts[] = array($i, (float)$v);
        }
    }
    $n = count($values);
    if (count($pts) < 2 || $n < 2) {
        return '';
    }
    $w = 100.0;
    $h = 32.0;
    $padX = 3.0;
    $padY = 5.0;
    $min = $pts[0][1];
    $max = $pts[0][1];
    foreach ($pts as $p) {
        $min = min($min, $p[1]);
        $max = max($max, $p[1]);
    }
    if ($zeroBase) {
        $min = min($min, 0.0);
    }
    $span = $max - $min > 0 ? $max - $min : 1.0;
    $coords = array();
    foreach ($pts as $p) {
        $x = $padX + ($w - 2 * $padX) * $p[0] / ($n - 1);
        $y = $h - $padY - ($h - 2 * $padY) * ($p[1] - $min) / $span;
        $coords[] = round($x, 2) . ',' . round($y, 2);
    }
    $last = explode(',', end($coords));
    $dot = 'M' . $last[0] . ' ' . $last[1] . 'h0'; // zero-length path + round cap = a dot that never stretches
    return '<svg class="spark" viewBox="0 0 100 32" preserveAspectRatio="none" aria-hidden="true">'
        . '<polyline points="' . implode(' ', $coords) . '" fill="none"/>'
        . '<path class="spark-ring" d="' . $dot . '"/><path class="spark-dot" d="' . $dot . '"/></svg>';
}
