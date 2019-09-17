//= require jquery
//= require bootstrap

$(function() {
  $('.evaluation[data-running=true]').each(function() {
    const taskId = $(this).attr("data-task-id");
    setTimeout(refreshEvaluationStatus(taskId), 3000)
  });
});

const refreshEvaluationStatus = taskId => () => {
  $.get("/tasks/" + taskId + "/evaluation", res => {
    if (res.running) {
      setTimeout(refreshEvaluationStatus(taskId), 3000)
    } else {
      const evaluation = $('.evaluation[data-task-id=' + taskId +']');
      evaluation.find(".evaluation-running").html('<i class="fas fa-check"></i> Finished');
      evaluation.find(".evaluation-view")
        .attr('title', '')
        .attr('data-original-title', '')
        .tooltip('dispose')
        .tooltip()
        .children()
        .first()
        .attr("href", res.url)
        .removeClass("disabled");

      const message = `<p>Evaluation for task '${res.taskTitle}' has finished.</p><a href="${res.url}" class="btn btn-sm btn-light"><i class="fas fa-poll"></i> Show Results</a>`;
      showToast('Evaluation finished', 'fas fa-check text-success', message);
    }
  })
};
