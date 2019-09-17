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

$('[data-toggle="tooltip"]').tooltip()

function showToast(title, icon, content) {
  const now = new Date();
  $('#toast-container').append(`
    <div class="toast" role="alert" aria-live="assertive" aria-atomic="true" data-delay="10000">
      <div class="toast-header">
        <i class="mr-1 ${icon}"></i>
        <strong class="mr-auto">${title}</strong>
        <small class="text-muted">${now.getHours()}:${now.getMinutes().pad()}</small>
        <button type="button" class="ml-2 mb-1 close" data-dismiss="toast" aria-label="Close">
          <span aria-hidden="true">&times;</span>
        </button>
      </div>
      <div class="toast-body">${content}</div>
    </div>
  `).children().last().toast('show')
}

Number.prototype.pad = function(size) {
  let s = String(this);
  while (s.length < (size || 2)) {s = "0" + s;}
  return s;
}

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
