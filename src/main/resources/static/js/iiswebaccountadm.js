
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

        var commObjListStr = iisWebObj.commObjListStr;

        if (commObjListStr !== "") {
            var commObjList = JSON.parse(commObjListStr);

            var htmlhead = '<div class="ui-grid-b">';
            htmlhead += '<div class="ui-block-a" style="width:30%"><strong>Date</strong></div>';
            htmlhead += '<div class="ui-block-b" style="width:5%"></div>';
            htmlhead += '<div class="ui-block-c">Msg</div>';
            htmlhead += '</div>';

            $("#msgid").html('<li id="0" >' + htmlhead + '</li>');

            for (i = 0; i < commObjList.length; i++) {
                var commObj = commObjList[i];
                var commId = commObj.id;

                var htmlName = '<div class="ui-grid-b">';
                htmlName += '<div class="ui-block-a" style="width:30%"><strong>' + commObj.updatedatedisplay + '</strong></div>';
                htmlName += '<div class="ui-block-b" style="width:5%"> </div>';
                htmlName += '<div class="ui-block-c">' + commObj.data + '</div>';
                htmlName += '</div>';

                $("#msgid").append('<li id="' + commId + '" >' + htmlName + '</li>');

            }
        }

        $("#accheader").html("Admin Control");

        $("#myid").html(" "); //clear the field
        var htmlhead = '<div class="ui-grid-b">';
        htmlhead += '<div class="ui-block-a" style="width:15%"><strong>Date</strong></div>';
        htmlhead += '<div class="ui-block-b" style="width:5%">Id</div>';
        htmlhead += '<div class="ui-block-c">Msg</div>';
        htmlhead += '</div>';

        $("#myid").html('<li id="0" >' + htmlhead + '</li>');

        if (commObjListStr !== "") {
            var commObjList = JSON.parse(commObjListStr);

            for (i = 0; i < commObjList.length; i++) {
                var commObj = commObjList[i];
                var commId = commObj.id;

                var htmlName = '<div class="ui-grid-b">';
                htmlName += '<div class="ui-block-a" style="width:15%"><strong>' + commObj.updatedatedisplay + '</strong></div>';
                htmlName += '<div class="ui-block-b" style="width:5%">' + commId + '</div>';
                htmlName += '<div class="ui-block-c">' + commObj.name + ' ' + commObj.data + '</div>';
                htmlName += '</div>';

                $("#myid").append('<li id="' + commId + '" >' + htmlName + '</li>');

            }
        }



        $("ul[id*=myid] li").click(function () {
//            alert($(this).html()); // gets innerHTML of clicked li
//            alert($(this).text()); // gets text contents of clicked li
            var Id = $(this).attr('id');
            console.log(Id);
            if (Id === 0) {
//                alert(accId);
                return;
            }

            return;
//            var iisWebObj = {'custObjStr': custObjStr, 'accObjListStr': accObjListStr};
//            window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
//            window.location.href = "#";
        });




        $("#removemsgidsubmit").click(function () {
            var msgid = document.getElementById("removemsgid").value;
            if (msgid === "") {
                window.location.href = "accountadm.html";
                return;
            }
            ///cust/{username}/acc/{accountid}/comm/remove/{id}
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/acc/" + accObj.id + "/comm/remove/" + msgid,
                crossDomain: true,
                cache: false,
                success: handleResult
            }); // use promises

            // add cordova progress indicator https://www.npmjs.com/package/cordova-plugin-progress-indicator
            function handleResult(result) {
                console.log(result);
                alert("Remove Message Id result: " + result);
                window.location.href = "accountadm_1.html";
            }
        });

        $("#custnamesubmit").click(function () {
            var username = document.getElementById("custname").value;
            if (username === "") {
                window.location.href = "accountadm.html";
                return;
            }
            var cuObj = {'cmd': 'name', 'username': username,
                'firstid': '0', 'lastid': '0'};
            var cuObjStr = JSON.stringify(cuObj);

            var iisWebObj = {'custObjStr': custObjStr, 'accObjListStr': accObjListStr,
                'cuObjStr': cuObjStr};
            window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

            window.location.href = "accountadmcu_1.html";

        });

        $("#nextCust").click(function () {

            // cust/{username}/uisys/{custid}/custnlist?length={0 for all} - default 20");
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id + "/custnlist?length=0",
                crossDomain: true,
                cache: false,
                success: handleResult
            }); // use promises

            // add cordova progress indicator https://www.npmjs.com/package/cordova-plugin-progress-indicator
            function handleResult(resultCustNList) {
                console.log(resultCustNList);
                var CustNListStr = JSON.stringify(resultCustNList, null, '\t');
                var iisWebObj = {'custObjStr': custObjStr, 'accObjListStr': accObjListStr, 'commObjListStr': commObjListStr,
                    'CustNListStr': CustNListStr};
                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
                window.location.href = "accountadm.html";
            }

        });



// example        
//alert("AJAX request successfully completed");
//var jsonObj = JSON.parse(jsonStr);
//var jsonPretty = JSON.stringify(jsonObj, null, '\t');

    }
};
app.initialize();





