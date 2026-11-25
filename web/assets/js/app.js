/* TimSales web — progressive enhancements. Plain ES5, no framework. */
(function () {
  'use strict';

  function $(sel, root) { return (root || document).querySelector(sel); }
  function each(sel, fn) { Array.prototype.forEach.call(document.querySelectorAll(sel), fn); }
  function num(v) { var n = parseFloat(String(v).replace(/,/g, '')); return isNaN(n) ? 0 : n; }
  function fmt(n, digits) {
    var d = digits === undefined ? 2 : digits;
    return Number(n || 0).toLocaleString('en-US', { minimumFractionDigits: d, maximumFractionDigits: d });
  }
  function readJson(id) {
    var el = document.getElementById(id);
    try { return el ? JSON.parse(el.textContent || 'null') : null; } catch (e) { return null; }
  }

  // ---------- Generic ----------
  // Season links: keep the current one in view when the row scrolls sideways
  each('.season-tabs [aria-current]', function (el) {
    var row = el.parentNode.parentNode;
    row.scrollLeft = el.offsetLeft - (row.clientWidth - el.offsetWidth) / 2;
  });

  // "ใช้ราคาล่าสุด": fill the price only when the user taps it
  each('[data-fill-price]', function (btn) {
    var price = document.getElementById('unitPrice');
    if (!price) { return; }
    function sync() { btn.hidden = price.value.trim() !== ''; }
    btn.addEventListener('click', function () {
      price.value = btn.getAttribute('data-fill-price');
      price.dispatchEvent(new Event('input', { bubbles: true }));
      price.focus();
    });
    price.addEventListener('input', sync);
    sync();
  });

  each('form[data-confirm]', function (form) {
    form.addEventListener('submit', function (e) {
      if (!window.confirm(form.getAttribute('data-confirm'))) { e.preventDefault(); }
    });
  });

  // Disable the submit button once, so a slow save can't be sent twice
  each('form.form', function (form) {
    form.addEventListener('submit', function () {
      var btns = form.querySelectorAll('button[type=submit]');
      setTimeout(function () { Array.prototype.forEach.call(btns, function (b) { b.disabled = true; }); }, 0);
    });
  });

  // Success toasts: fade out after a few seconds, or right away on tap
  each('[data-toasts] .toast', function (toast, i) {
    var gone = false;
    function dismiss() {
      if (gone) { return; }
      gone = true;
      toast.className += ' is-out';
      setTimeout(function () { if (toast.parentNode) { toast.parentNode.removeChild(toast); } }, 250);
    }
    toast.addEventListener('click', dismiss);
    setTimeout(dismiss, 4500 + i * 1000);
  });

  each('.file-drop input[type=file]', function (input) {
    var label = input.parentNode;
    var name = label.querySelector('[data-file-name]');
    input.addEventListener('change', function () {
      var f = input.files && input.files[0];
      name.textContent = f ? f.name : 'เลือกไฟล์ .csv';
      label.className = f ? 'file-drop has-file' : 'file-drop';
    });
  });

  // ---------- Sale form: live total + which season the date belongs to ----------
  each('form[data-sale-form]', function (form) {
    var kg = form.querySelector('[name=kg]');
    var price = form.querySelector('[name=unitPrice]');
    var date = form.querySelector('[name=saleDate]');
    var total = form.querySelector('[data-total]');
    var hint = form.querySelector('[data-season-hint]');
    var ranges = readJson('seasonRanges') || []; // sorted by start: the earliest start wins, as on the server

    function updateTotal() { total.textContent = fmt(num(kg.value) * num(price.value)); }
    function updateSeason() {
      var d = date.value, match = null;
      for (var i = 0; i < ranges.length; i++) {
        if (d >= ranges[i].start && d <= ranges[i].end) { match = ranges[i]; break; }
      }
      if (!d) { hint.textContent = ''; hint.className = 'field-hint'; return; }
      hint.textContent = match ? 'นับเข้ารอบ ' + match.code : 'วันที่นี้ไม่อยู่ในรอบการขายใด — จะไม่แสดงในภาพรวม';
      hint.className = 'field-hint ' + (match ? 'is-ok' : 'is-warn');
    }
    kg.addEventListener('input', updateTotal);
    price.addEventListener('input', updateTotal);
    // Enter on the keypad in "kg" moves on to the price instead of saving a sale with no price
    kg.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' || e.keyCode === 13) { e.preventDefault(); price.focus(); }
    });
    date.addEventListener('change', updateSeason);
    date.addEventListener('input', updateSeason);
    updateTotal();
    updateSeason();
  });

  // ---------- Season form: code preview + default dates that follow the year ----------
  each('form[data-season-form]', function (form) {
    var year = form.querySelector('[name=saleYear]');
    var start = form.querySelector('[name=startDate]');
    var end = form.querySelector('[name=endDate]');
    var code = form.querySelector('[data-season-code]');

    function defaults(y) { return { start: y + '-02-01', end: (y + 1) + '-03-31' }; }
    function validYear() { var y = parseInt(year.value, 10); return (y >= 1900 && y <= 2100) ? y : null; }
    var y0 = validYear();
    // Dates follow the year only while they still hold the defaults
    var follow = y0 !== null && start.value === defaults(y0).start && end.value === defaults(y0).end;

    function update() {
      var y = validYear();
      code.textContent = y === null ? '–' : y + '/' + ('0' + ((y + 1) % 100)).slice(-2);
      if (y !== null && follow) {
        var d = defaults(y);
        start.value = d.start;
        end.value = d.end;
      }
    }
    year.addEventListener('input', update);
    start.addEventListener('input', function () { follow = false; });
    end.addEventListener('input', function () { follow = false; });
    update();
  });

  // ---------- Dashboard chart ----------
  var points = readJson('monthlyData');
  var card = $('[data-chart-card]');
  if (!points || !card || !window.Chart) { return; }
  var Chart = window.Chart;

  var METRICS = {
    revenue: { title: 'ยอดขายรายเดือน', unit: 'บาท', type: 'bar', digits: 2 },
    kg: { title: 'ปริมาณรายเดือน', unit: 'กก.', type: 'bar', digits: 2 },
    price: { title: 'ราคาเฉลี่ยรายเดือน', unit: 'บาท/กก.', type: 'line', digits: 2 }
  };
  var metric = 'revenue';
  try { metric = window.localStorage.getItem('timsales.metric') || 'revenue'; } catch (e) { /* storage blocked */ }
  if (!METRICS[metric]) { metric = 'revenue'; }

  function cssVar(name) { return window.getComputedStyle(document.documentElement).getPropertyValue(name).trim(); }
  function palette() {
    return {
      series: cssVar('--series'), seriesMuted: cssVar('--series-muted'), grid: cssVar('--grid'),
      axis: cssVar('--axis'), text: cssVar('--text'), text2: cssVar('--text-2'),
      surface: cssVar('--surface'), line: cssVar('--line-strong')
    };
  }

  Chart.defaults.font.family = window.getComputedStyle(document.body).fontFamily;
  Chart.defaults.font.size = 12;

  // Axis ticks: in full under 10,000, compact (12K / 1.2M) once the axis reaches it
  function compact(v, axisMax) {
    var n = Number(v);
    if (n === 0) { return '0'; }
    if (axisMax >= 1e6) { return +(n / 1e6).toFixed(1) + 'M'; }
    if (axisMax >= 1e4) { return +(n / 1e3).toFixed(1) + 'K'; }
    return n.toLocaleString('en-US', { maximumFractionDigits: 2 });
  }
  function compactLabel(n) {
    if (n >= 999500) { return +(n / 1e6).toFixed(1) + 'M'; }
    if (n >= 1e4) { return +(n / 1e3).toFixed(1) + 'K'; }
    return n.toLocaleString('en-US', { maximumFractionDigits: 2 });
  }

  // Phones: a 14-month season leaves ~13px per bar. In the card chart, drop the months
  // still ahead and label only the peak; every value stays in the tooltip, the table
  // view and the fullscreen chart.
  var narrowMq = window.matchMedia ? window.matchMedia('(max-width: 519px)') : null;
  function compactChart(big) { return !big && !!(narrowMq && narrowMq.matches); }
  function shownPoints(big) {
    if (!compactChart(big)) { return points; }
    var n = points.length;
    while (n > 0 && points[n - 1].future) { n--; }
    return n > 0 ? points.slice(0, n) : points; // trailing only, so indexes still match points[]
  }

  // The month the "this month" card shows: full colour; the others step back (bars only)
  var nowMonth = card.getAttribute('data-now-month') || '';
  function barColors(pts, col) {
    var hasNow = pts.some(function (p) { return p.yearMonth === nowMonth; });
    return pts.map(function (p) { return !hasNow || p.yearMonth === nowMonth ? col.series : col.seriesMuted; });
  }

  function seriesValues(key, pts) {
    return pts.map(function (p) {
      if (p.future) { return null; }
      return key === 'price' ? p.price : p[key];
    });
  }

  // Tooltip pinned to the top of the plot so it never covers the marks
  Chart.Tooltip.positioners.top = function (items) {
    if (!items.length) { return false; }
    return { x: items[0].element.x, y: this.chart.chartArea.top, xAlign: 'center', yAlign: 'top' };
  };

  // Vertical hairline under the hovered month (line chart)
  var crosshair = {
    id: 'crosshair',
    beforeDatasetsDraw: function (chart, args, opts) {
      var active = chart.tooltip && chart.tooltip.getActiveElements();
      if (!opts.enabled || !active || !active.length) { return; }
      var x = active[0].element.x, area = chart.chartArea, c = chart.ctx;
      c.save();
      c.beginPath();
      c.moveTo(x, area.top);
      c.lineTo(x, area.bottom);
      c.lineWidth = 1;
      c.strokeStyle = opts.color;
      c.stroke();
      c.restore();
    }
  };

  // Label only the peak month — every other value lives in the tooltip and the table view
  var peakLabel = {
    id: 'peakLabel',
    afterDatasetsDraw: function (chart, args, opts) {
      var values = chart.data.datasets[0].data, best = -1;
      for (var i = 0; i < values.length; i++) {
        if (values[i] !== null && values[i] > 0 && (best < 0 || values[i] > values[best])) { best = i; }
      }
      if (best < 0) { return; }
      var el = chart.getDatasetMeta(0).data[best];
      var c = chart.ctx, t = compactLabel(values[best]);
      c.save();
      c.font = '600 12px ' + Chart.defaults.font.family;
      c.textAlign = 'center';
      c.textBaseline = 'bottom';
      c.lineJoin = 'round';
      c.lineWidth = 4;
      c.strokeStyle = opts.halo;
      c.fillStyle = opts.color;
      var y = el.y - (opts.type === 'line' ? 10 : 6);
      c.strokeText(t, el.x, y);
      c.fillText(t, el.x, y);
      c.restore();
    }
  };

  // A value above every bar. One style for the whole chart, picked to fit the bar slot:
  // full numbers → compact (28.5K) → compact turned vertical. Zero and future months get no label.
  var LABEL_FONT = 11;
  function fullLabel(n) { return Number(n).toLocaleString('en-US', { maximumFractionDigits: 1 }); }
  var barValues = {
    id: 'barValues',
    beforeLayout: function (chart) {
      var values = chart.data.datasets[0].data, c = chart.ctx, widest = { full: 0, compact: 0 };
      c.save();
      c.font = '600 ' + LABEL_FONT + 'px ' + Chart.defaults.font.family;
      for (var i = 0; i < values.length; i++) {
        if (!values[i]) { continue; }
        widest.full = Math.max(widest.full, c.measureText(fullLabel(values[i])).width);
        widest.compact = Math.max(widest.compact, c.measureText(compactLabel(values[i])).width);
      }
      c.restore();
      // Estimated slot per month: canvas width minus the y-axis band, split across the months
      var slot = (chart.width - 56) / Math.max(values.length, 1) - 4;
      var mode = widest.full <= slot ? 'full' : (widest.compact <= slot ? 'compact' : 'vertical');
      chart.$barValueMode = mode;
      chart.options.layout.padding.top = mode === 'vertical' ? Math.ceil(widest.compact) + 10 : LABEL_FONT + 10;
    },
    afterDatasetsDraw: function (chart, args, opts) {
      var values = chart.data.datasets[0].data, mode = chart.$barValueMode, c = chart.ctx;
      var format = mode === 'full' ? fullLabel : compactLabel;
      c.save();
      c.font = '600 ' + LABEL_FONT + 'px ' + Chart.defaults.font.family;
      c.fillStyle = opts.color;
      c.strokeStyle = opts.halo;
      c.lineWidth = 2;
      c.lineJoin = 'round';
      chart.getDatasetMeta(0).data.forEach(function (bar, i) {
        if (!values[i]) { return; }
        var t = format(values[i]);
        if (mode === 'vertical') {
          c.save();
          c.translate(bar.x, bar.y - 4);
          c.rotate(-Math.PI / 2);
          c.textAlign = 'left';
          c.textBaseline = 'middle';
          c.strokeText(t, 0, 0);
          c.fillText(t, 0, 0);
          c.restore();
        } else {
          c.textAlign = 'center';
          c.textBaseline = 'bottom';
          c.strokeText(t, bar.x, bar.y - 4);
          c.fillText(t, bar.x, bar.y - 4);
        }
      });
      c.restore();
    }
  };

  function config(key, big) {
    var m = METRICS[key], col = palette(), isLine = m.type === 'line';
    var pts = shownPoints(big), values = seriesValues(key, pts);
    var dataset = isLine ? {
      data: values,
      borderColor: col.series,
      borderWidth: 2,
      borderCapStyle: 'round',
      borderJoinStyle: 'round',
      tension: 0, // monthly averages are discrete points: straight segments, no invented curve
      spanGaps: true,
      fill: false,
      pointRadius: function (ctx) { return values[ctx.dataIndex] === null ? 0 : 4; },
      pointHoverRadius: 6,
      pointHitRadius: 18,
      pointBackgroundColor: col.series,
      pointBorderColor: col.surface,
      pointBorderWidth: 2
    } : {
      data: values,
      backgroundColor: barColors(pts, col),
      hoverBackgroundColor: col.series,
      borderWidth: 0,
      borderRadius: 4,
      borderSkipped: 'start',
      maxBarThickness: 24,
      categoryPercentage: 0.7
    };

    return {
      type: m.type,
      data: { labels: pts.map(function (p) { return p.label; }), datasets: [dataset] },
      plugins: isLine ? [crosshair, peakLabel] : (compactChart(big) ? [peakLabel] : [barValues]),
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 350 },
        layout: { padding: { top: 24, right: 4 } },
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: { display: false },
          crosshair: { enabled: isLine, color: col.line },
          peakLabel: { color: col.text, halo: col.surface, type: m.type },
          barValues: { color: col.text, halo: col.surface },
          tooltip: {
            position: 'top',
            backgroundColor: col.surface,
            borderColor: col.line,
            borderWidth: 1,
            titleColor: col.text,
            bodyColor: col.text2,
            titleFont: { weight: '600' },
            padding: 10,
            cornerRadius: 10,
            caretSize: 0,
            displayColors: false,
            callbacks: {
              title: function (items) { return points[items[0].dataIndex].title; },
              label: function (ctx) {
                if (ctx.parsed.y === null) { return 'ยังไม่มีข้อมูล'; }
                return fmt(ctx.parsed.y, m.digits) + ' ' + m.unit;
              },
              afterLabel: function (ctx) {
                var n = points[ctx.dataIndex].count;
                return n > 0 ? n + ' รายการ' : '';
              }
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            border: { color: col.grid },
            ticks: { color: col.axis, maxRotation: 0, autoSkip: true, autoSkipPadding: big ? 16 : 10, padding: 6 }
          },
          y: {
            beginAtZero: !isLine,
            grace: isLine ? '15%' : '10%',
            border: { display: false },
            grid: { color: col.grid, drawTicks: false },
            ticks: {
              color: col.axis,
              padding: 8,
              maxTicksLimit: big ? 7 : 5,
              callback: function (v) { return compact(v, this.max); }
            }
          }
        }
      }
    };
  }

  var canvas = document.getElementById('mainChart');
  var chartBox = $('[data-chart-box]', card);
  var tableBox = $('[data-table-box]', card);
  var segmented = $('.segmented', card);
  var titleEl = $('[data-chart-title]', card);
  var subEl = $('[data-chart-sub]', card);
  var viewBtn = $('[data-view-toggle]', card);
  var chart = null;

  function render() {
    var m = METRICS[metric];
    titleEl.textContent = m.title;
    subEl.textContent = 'หน่วย: ' + m.unit + (m.type === 'line' ? ' · ถ่วงน้ำหนักตามปริมาณ' : '');
    each('[data-metric]', function (b) { b.setAttribute('aria-pressed', b.getAttribute('data-metric') === metric ? 'true' : 'false'); });
    if (chart) { chart.destroy(); }
    chart = new Chart(canvas, config(metric, false));
  }

  each('[data-metric]', function (btn) {
    btn.addEventListener('click', function () {
      metric = btn.getAttribute('data-metric');
      try { window.localStorage.setItem('timsales.metric', metric); } catch (e) { /* storage blocked */ }
      render();
    });
  });

  // Table view: the full monthly numbers (a chart's accessible twin)
  viewBtn.addEventListener('click', function () {
    var showTable = viewBtn.getAttribute('aria-pressed') !== 'true';
    // The name stays "แสดงเป็นตาราง"; only aria-pressed changes, so it reads as an on/off switch
    viewBtn.setAttribute('aria-pressed', showTable ? 'true' : 'false');
    tableBox.hidden = !showTable;
    chartBox.hidden = showTable;
    segmented.hidden = showTable;
    subEl.hidden = showTable;
    titleEl.textContent = showTable ? 'สรุปรายเดือน' : METRICS[metric].title;
  });

  // Fullscreen (replaces ChartFullscreenActivity)
  var modal = document.getElementById('chartModal');
  var modalCanvas = document.getElementById('chartModalCanvas');
  var modalTitle = document.getElementById('chartModalTitle');
  var modalChart = null;
  var lastFocus = null;
  var inerted = [];

  // The dialog lives inside <main>; move it to <body> so everything else can be made inert
  document.body.appendChild(modal);

  // While open, the page behind can't be tabbed to, clicked or read (inert + aria-hidden fallback)
  function setBackgroundInert(on) {
    if (on) {
      inerted = Array.prototype.filter.call(document.body.children, function (el) {
        return el !== modal && el.tagName !== 'SCRIPT';
      });
    }
    inerted.forEach(function (el) {
      if (on) { el.setAttribute('inert', ''); el.setAttribute('aria-hidden', 'true'); }
      else { el.removeAttribute('inert'); el.removeAttribute('aria-hidden'); }
    });
    if (!on) { inerted = []; }
  }

  function openModal() {
    lastFocus = document.activeElement;
    modalTitle.textContent = METRICS[metric].title + ' (' + METRICS[metric].unit + ')';
    modal.hidden = false;
    document.body.style.overflow = 'hidden';
    setBackgroundInert(true);
    modalChart = new Chart(modalCanvas, config(metric, true));
    modal.querySelector('[data-close-modal]').focus();
  }
  function closeModal() {
    if (modalChart) { modalChart.destroy(); modalChart = null; }
    setBackgroundInert(false);
    modal.hidden = true;
    document.body.style.overflow = '';
    if (lastFocus) { lastFocus.focus(); }
  }
  // Focus trap for browsers without `inert`: Tab wraps inside the dialog
  modal.addEventListener('keydown', function (e) {
    if (e.key !== 'Tab') { return; }
    var f = modal.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
    if (!f.length) { e.preventDefault(); return; }
    var first = f[0], last = f[f.length - 1];
    if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
    else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
  });
  $('[data-fullscreen]', card).addEventListener('click', openModal);
  each('[data-close-modal]', function (btn) { btn.addEventListener('click', closeModal); });
  modal.addEventListener('click', function (e) { if (e.target === modal) { closeModal(); } });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape' && !modal.hidden) { closeModal(); } });

  // Re-read the colour tokens when the system switches between light and dark
  if (window.matchMedia) {
    var mq = window.matchMedia('(prefers-color-scheme: dark)');
    var onScheme = function () {
      render();
      if (modalChart) { modalChart.destroy(); modalChart = new Chart(modalCanvas, config(metric, true)); }
    };
    if (mq.addEventListener) { mq.addEventListener('change', onScheme); } else if (mq.addListener) { mq.addListener(onScheme); }
  }
  // Crossing the phone breakpoint (rotation, window resize) switches the card chart layout
  if (narrowMq) {
    if (narrowMq.addEventListener) { narrowMq.addEventListener('change', render); } else if (narrowMq.addListener) { narrowMq.addListener(render); }
  }

  render();
  if (document.fonts && document.fonts.ready) { document.fonts.ready.then(render); }
})();
