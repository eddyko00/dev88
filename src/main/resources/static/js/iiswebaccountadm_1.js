
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
        var accObj = null;
        for (i = 0; i < accObjList.length; i++) {
            var accObjTmp = accObjList[i];
            if (accObjTmp.type == 140) { //INT_ADMIN_ACCOUNT = 140;
                accObj = accObjTmp;
                break;
            }
        }
        if (accObj == null) {
            window.location.href = "index.html";
        }
        $.ajax({
            url: iisurl + "/cust/" + custObj.username + "/acc/" + accObj.id + "/comm",
            crossDomain: true,
            cache: false,
            beforeSend: function () {
                $("#loader").show();
            },

            success: function (resultCommObjList) {
                console.log(resultCommObjList);
                if (resultCommObjList !== "") {
                    var commObjListStr = JSON.stringify(resultCommObjList, null, '\t');
                    var iisWebObj = {'custObjStr': custObjStr, 'accObjListStr': accObjListStr, 'commObjListStr': commObjListStr};
                    window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
                    window.location.href = "accountadm.html";
                } else {
                    var commObjListStr = "";
                    var iisWebObj = {'custObjStr': custObjStr, 'accObjListStr': accObjListStr, 'commObjListStr': commObjListStr};
                    window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
                    window.location.href = "accountadm.html";
                }
            }
        });
    }
};
app.initialize();





