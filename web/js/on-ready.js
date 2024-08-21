function bindReady(handler){
    var called = false
    function ready() {
        if (called) return
        called = true
        handler()
    }
    if ( document.addEventListener ) {
        document.addEventListener( "DOMContentLoaded", function(){
            ready()
        }, false )
    } else if ( document.attachEvent ) {
        if ( document.documentElement.doScroll && window == window.top ) {
            function tryScroll(){
                if (called) return
                if (!document.body) return
                try {
                    document.documentElement.doScroll("left")
                    ready()
                } catch(e) {
                    setTimeout(tryScroll, 0)
                }
            }
            tryScroll()
        }
        document.attachEvent("onreadystatechange", function(){
            if ( document.readyState === "complete" ) {
                ready()
            }
        })
    }
    if (window.addEventListener)
        window.addEventListener('load', ready, false)
    else if (window.attachEvent)
        window.attachEvent('onload', ready)
    /*  else  // use this 'else' statement for very old browsers :)
        window.onload=ready
    */
}
readyList = []
function onReady(handler) {
    if (!readyList.length) {
        bindReady(function() {
            for(var i=0; i<readyList.length; i++) {
                readyList[i]()
            }
        })
    }
    readyList.push(handler)
}