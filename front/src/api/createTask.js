export function uploadTaskDescription(file, callback) {
    let formData = new FormData();
    formData.append('taskconf', file);

    // $.post('http://localhost:8000/naive/requester/check.html', formData, res => callback(res))
    $.ajax({
        url: 'http://localhost:8000/naive/requester/check.html',
        type: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        success: function (res) {
            callback(JSON.parse(res))
        }
    })
}

export function uploadTask(form,callback) {
     let formData=new FormData();

     let task={
         taskName:form.taskName,
         workerFilter:form.workerFilter,
         endTime:form.endTime,
         score:form.score,
     };

     formData.append('taskInfo',JSON.stringify(task));
    formData.append('dataset',form.file);
    formData.append('taskconf',form.taskConf);
    formData.append('username',localStorage.username);
    let username=localStorage.username;


     console.log(localStorage.username);

    $.ajax({
        url: 'http://localhost:8000/naive/requester/new.html',
        type: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        success: function (res) {
           callback((JSON).parse(res));
        }
    })

}

export function createTask(taskForm,callback){

}