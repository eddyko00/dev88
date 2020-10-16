
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
        var accId = iisWebObj.accId;
        console.log(accId);
        $.ajax({
            url: iisurl + "/cust/" + custObj.username + "/acc/" + accId + "/st",
            crossDomain: true,
            cache: false,
            beforeSend: function () {
                 $("#loader").show();
            },

            error: function () {
                window.location.href = "index.html";
            },

            success: function (resultStockList) {
                console.log(resultStockList);
                var stockObjListStr = JSON.stringify(resultStockList, null, '\t');
                var iisWebObj = {'custObjStr': custObjStr, 'accObjListStr': accObjListStr, 'accId': accId, 'stockObjListStr': stockObjListStr};
                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
                window.location.href = "accountst.html";
            }
        });
    }
};
app.initialize();





