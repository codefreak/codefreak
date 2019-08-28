//= require jquery

$(function() {
  $('.start-evaluation').on('click', function() {
    $.post('/evaluations', {
      taskId: $(this).attr('data-task-id'),
      [$('meta[name=_csrf_parameter]').attr('content')]: $('meta[name=_csrf]').attr('content')
    })
  })
});
