
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

        var accObj = null;
        for (i = 0; i < accObjList.length; i++) {
            var accObjTmp = accObjList[i];
            if (accObjTmp.id == accId) {
                accObj = accObjTmp;
                break;
            }
        }
        if (accObj == null) {
            window.location.href = "index.html";
        }

        var stockObjListStr = iisWebObj.stockObjListStr;
        var stockObjList = JSON.parse(stockObjListStr);
        var sockId = iisWebObj.sockId;
        console.log(sockId);

        var stockObj = null;
        for (i = 0; i < stockObjList.length; i++) {
            var stockObjTmp = stockObjList[i];
            if (stockObjTmp.id == sockId) {
                stockObj = stockObjTmp;
                break;
            }
        }
        if (stockObj == null) {
            window.location.href = "index.html";
        }

        $.ajax({
            url: iisurl + "/cust/" + custObj.username + "/acc/" + accId + "/st/" + sockId + "/tr",
            crossDomain: true,
            cache: false,
            beforeSend: function () {
                 $("#loader").show();
            },         
            
            error: function () {
                window.location.href = "index.html";
            },

            success: function (resultTRList) {
                console.log(resultTRList);

                var trObjListStr = JSON.stringify(resultTRList, null, '\t');
                var iisWebObj = {'custObjStr': custObjStr, 'accObjListStr': accObjListStr,
                    'accId': accId, 'stockObjListStr': stockObjListStr, 'sockId': sockId, 'trObjListStr': trObjListStr};
                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

                window.location.href = "accountsttr.html";
            }
        });

    }
};
app.initialize();





