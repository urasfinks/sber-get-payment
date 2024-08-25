window.$$ = function (id) {
    return document.getElementById(id);
}

function ajax(url, callback) {
    $$("error").innerHTML = "";
    var xmlhttp = new XMLHttpRequest();

    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == XMLHttpRequest.DONE) { // XMLHttpRequest.DONE == 4
            if (xmlhttp.status == 200) {
                var data = JSON.parse(xmlhttp.responseText);
                callback(data);
            } else if (xmlhttp.status == 0) {
                $$("error").innerHTML = 'Server error';
            }
        }
    };

    xmlhttp.open("GET", url, true);
    xmlhttp.send();
}

window.blank = function (url) {
    var anchor = document.createElement('a');
    anchor.href = url;
    anchor.target = "_blank";
    anchor.click();
}

window.formDataSubmit = function () {
    $$("form-data").submit();
}

window.openPdf = function(action){
    var form = $$("pdf");
    form.action = action;
    $$("pdf-value").value = JSON.stringify(window.payData);
    form.submit();
}

function isEmpty(obj) {
    for (const prop in obj) {
        if (Object.hasOwn(obj, prop)) {
            return false;
        }
    }

    return true;
}

onReady(function () {
    if(window.payData == undefined || window.payData == null || isEmpty(window.payData)){
        return;
    }
    $$("info").style.display="block";
    for(var key in window.payData){
        var obj = $$(key);
        if(obj != undefined && obj != null){
            obj.innerHTML = window.payData[key];
        }
    }
})