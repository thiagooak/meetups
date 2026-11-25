(() => {
  'use strict';

  // Mailchimp sends people back with ?status=subscribed
  const status = new URLSearchParams(window.location.search).get('status');
  document.querySelectorAll('[data-status]').forEach((notice) => {
    notice.hidden = notice.dataset.status !== status;
  });
})();
