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

document.getElementById("form-data").onkeydown = function (evt) {
    var keyCode = evt ? (evt.which ? evt.which : evt.keyCode) : event.keyCode;
    if (keyCode == 13) {
        window.formDataSubmit();
    }
};

onReady(function () {
    setTimeout(function () {
        var suip = $$("suip").value;
        if (suip != undefined && suip.trim() !== "") {
            window.formDataSubmit();
        }
    }, 1000);
})