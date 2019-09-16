//= require bootstrap
//= require jquery

// fix modals by moving them to the end of <body>
$('.modal').each(function () {
  $(this).detach().appendTo('body');
});

// show file name in custom file inputs
$('.custom-file-input').each(function () {
  var $label = $(this).next('.custom-file-label');
  var originalLabel = $label.text();
  $(this).on('change', function () {
    $label.text(this.files.length ? this.files[0].name : originalLabel)
  });
});

// focus username and or password field if login is present
$(function () {
  var username = $('#username');
  var password = $('#password');
  if (!username.length || !password.length) {
    return;
  } else if (!username.val()) {
    username.focus()
  } else {
    password.focus();
  }
});
