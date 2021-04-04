
var app = {

// Application Constructor
    initialize: function () {

        $(document).ready(function () {

        });

//        var iisurl = "https://iiswebsrv.herokuapp.com/";
        var iisMsgSession = "iisMsgSession";
        var iisWebSession = "iisWebSession";
//        var custObj = 'custObj';
//        var accList = 'accList';

        var iisWebObjStr = window.localStorage.getItem(iisWebSession);
        var iisWebObj = JSON.parse(iisWebObjStr);
//        console.log(iisWebObj);
        var iisurlStr = iisWebObj.iisurlStr;
        iisurl = iisurlStr;

        var custObjStr = iisWebObj.custObjStr;
        if (custObjStr == null) {
            window.location.href = "index.html";
        }
        var custObj = JSON.parse(custObjStr);
        var accObjListStr = iisWebObj.accObjListStr;
        var accObjList = JSON.parse(accObjListStr);
        var accId = iisWebObj.accId;
        console.log(accId);

        var trName = "TR_ACC";
        if (iisWebObj.trName != null) {
            trName = iisWebObj.trName;
        }

        var trFilter = "";
        if (iisWebObj.trFilter != null) {
            trFilter = iisWebObj.trFilter;
        }
        $.ajax({
            url: iisurl + "/cust/" + custObj.username + "/acc/" + accId + "/st?trname=" + trName + "&filter=" + trFilter,
            crossDomain: true,
            cache: false,
            beforeSend: function () {
                $("#loader").show();
            },
            error: function () {
                alert('Network failure. Please try again later.');
                window.location.href = "index.html";
            },
            success: function (resultStockList) {
                console.log(resultStockList);
                window.localStorage.setItem(iisMsgSession, "");

                var stockObjListStr = JSON.stringify(resultStockList, null, '\t');
                var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
                    'accId': accId, 'trFilter': trFilter, 'stockObjListStr': stockObjListStr, 'trName': trName};
                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
                window.location.href = "accountst.html";
            }
        });
    }
};
app.initialize();





