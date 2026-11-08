(function () {
  'use strict';

  // Mobile sidebar toggle
  var toggle = document.getElementById('navToggle');
  var sidebar = document.getElementById('sidebar');
  if (toggle && sidebar) {
    toggle.addEventListener('click', function () { sidebar.classList.toggle('open'); });
    document.addEventListener('click', function (e) {
      if (sidebar.classList.contains('open') && !sidebar.contains(e.target) && !toggle.contains(e.target)) sidebar.classList.remove('open');
    });
  }

  // Success messages fade out on their own; errors stay until dismissed
  document.querySelectorAll('.alert-auto').forEach(function (el) {
    setTimeout(function () { el.classList.add('d-none'); }, 6000);
  });

  // Ask before destructive actions: <form data-confirm="Are you sure?">
  document.querySelectorAll('form[data-confirm]').forEach(function (form) {
    form.addEventListener('submit', function (e) {
      if (!window.confirm(form.getAttribute('data-confirm'))) e.preventDefault();
    });
  });

  // Prevent double submits
  document.querySelectorAll('form[data-once]').forEach(function (form) {
    form.addEventListener('submit', function () {
      form.querySelectorAll('button[type=submit]').forEach(function (b) { b.disabled = true; });
    });
  });

  // Live clock on the attendance pages
  var clock = document.getElementById('liveClock');
  if (clock) {
    var tick = function () { clock.textContent = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }); };
    tick();
    setInterval(tick, 1000);
  }

  // Leave form: working-day estimate (Mon-Fri) and sensible end-date minimum
  var start = document.getElementById('startDate');
  var end = document.getElementById('endDate');
  var out = document.getElementById('dayEstimate');
  if (start && end && out) {
    var estimate = function () {
      if (start.value) end.min = start.value;
      if (!start.value || !end.value) { out.textContent = '-'; return; }
      var s = new Date(start.value + 'T00:00:00'), e = new Date(end.value + 'T00:00:00');
      if (e < s) { out.textContent = 'End date is before start date'; return; }
      var n = 0;
      for (var d = new Date(s); d <= e; d.setDate(d.getDate() + 1)) {
        var w = d.getDay();
        if (w !== 0 && w !== 6) n++;
      }
      out.textContent = n + (n === 1 ? ' working day' : ' working days');
    };
    start.addEventListener('change', function () { if (!end.value || end.value < start.value) end.value = start.value; estimate(); });
    end.addEventListener('change', estimate);
    estimate();
  }
})();
