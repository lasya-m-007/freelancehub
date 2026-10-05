// Confirm-before-delete: any <form data-confirm="Are you sure?"> asks first.
document.addEventListener('submit', function (e) {
  var msg = e.target.getAttribute && e.target.getAttribute('data-confirm');
  if (msg && !window.confirm(msg)) { e.preventDefault(); }
});

// Bootstrap client-side validation hints for <form class="needs-validation" novalidate>.
document.querySelectorAll('form.needs-validation').forEach(function (form) {
  form.addEventListener('submit', function (e) {
    if (!form.checkValidity()) { e.preventDefault(); e.stopPropagation(); }
    form.classList.add('was-validated');
  }, false);
});
