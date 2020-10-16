
var app = {

// Application Constructor
    initialize: function () {

        $(document).ready(function () {

        });

//        var iisurl = "https://iiswebsrv.herokuapp.com/";
        var iisWebSession = "iisWebSession";
//        var custObj = 'custObj';
//        var accList = 'accList';

        var iisWebObjStr = window.localStorage.getItem(iisWebSession);
        var iisWebObj = JSON.parse(iisWebObjStr);
        console.log(iisWebObj);

        var custObjStr = iisWebObj.custObjStr;
        if (custObjStr == null) {
            window.location.href = "index.html";
        }
        var custObj = JSON.parse(custObjStr);
        var accObjListStr = iisWebObj.accObjListStr;
        var accObjList = JSON.parse(accObjListStr);


        $.ajax({
            url: iisurl + "cust/" + custObj.username + "/sys/lock",

            crossDomain: true,
            cache: false,
            beforeSend: function () {
                $("#loader").show();
            },

            error: function () {
                alert('network failure');
                window.location.href = "index.html";
            },
            success: function (lockObjList) {
                console.log(lockObjList);
                var lockObjListStr = JSON.stringify(lockObjList, null, '\t');


                $.ajax({
                    url: iisurl + "server",

                    crossDomain: true,
                    cache: false,
                    success: function (serverList) {
                        var serverListStr = JSON.stringify(serverList, null, '\t');
                        var iisWebObj = {'custObjStr': custObjStr, 'accObjListStr': accObjListStr,
                            'lockObjListStr': lockObjListStr, 'serverListStr': serverListStr};
                        window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
                        $.ajax({
                            url: iisurl + "timerhandler?resttimerMsg=starttimer",

                            crossDomain: true,
                            cache: false,
                            success: function (timer) {
                                window.location.href = "accountstatus.html";
                                return;
                            }
                        });
                        return;
                    }
                });
                return;
            }
        });

    }
};
app.initialize();





