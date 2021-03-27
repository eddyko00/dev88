
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
        var fundObjListStr = iisWebObj.fundObjListStr;  
        var fundObjList = JSON.parse(fundObjListStr);
        var commObjListStr = iisWebObj.commObjListStr;

        var iisMsgSession = "iisMsgSession";
        var msgObjStr = window.localStorage.getItem(iisMsgSession);
//        msgObjStr ="This feature does not allow for GUEST account";
        if (msgObjStr !== "") {
            functionAlertConfirm(msgObjStr, function ok() {
            });
        }

//        if (commObjListStr !== "") {
//            var commObjList = JSON.parse(commObjListStr);
//
//            var htmlhead = '<div class="ui-grid-b">';
//            htmlhead += '<div class="ui-block-a" style="width:30%"><strong>Date</strong></div>';
//            htmlhead += '<div class="ui-block-b" style="width:5%"></div>';
//            htmlhead += '<div class="ui-block-c">Msg</div>';
//            htmlhead += '</div>';
//
//            $("#msgid").html('<li id="0" >' + htmlhead + '</li>');
//
//            for (i = 0; i < commObjList.length; i++) {
//                var commObj = commObjList[i];
//                var commId = commObj.id;
//
//                var htmlName = '<div class="ui-grid-b">';
//                htmlName += '<div class="ui-block-a" style="width:30%"><strong>' + commObj.updatedatedisplay + '</strong></div>';
//                htmlName += '<div class="ui-block-b" style="width:5%"> </div>';
//                htmlName += '<div class="ui-block-c">' + commObj.data + '</div>';
//                htmlName += '</div>';
//
//                $("#msgid").append('<li id="' + commId + '" >' + htmlName + '</li>');
//
//            }
//        }


        $("#accheader").html("Fund Mgr Account" + ' ' + '<a href="#page-intro"><small>Help</small></a>');

        $("#myid").html(" "); //clear the field
        for (i = 0; i < fundObjList.length; i++) {
            var accObj = fundObjList[i];
            console.log(accObj);
            var accName = accObj.accountname;
            var accId = accObj.id;

            var htmlName = '';
            htmlName += '<li id="' + accId + '"><a href="#">';
            htmlName += '<br>Account: ' + accName;
            if (accObj.type == 110) { //INT_TRADING_ACCOUNT           
                var pp = "Basic Plan - Max 2 stocks";
                if (custObj.substatus == 0) {
                    pp = "Basic Plan - Max 2 stocks";
                } else if (custObj.substatus == 10) {
                    pp = "Premium Plan - Max 10 stocks";
                } else if (custObj.substatus == 20) {
                    pp = "Deluxe Plan - Max 20 stocks";
                }

                htmlName += '<br>Plan: ' + pp;
                var balanceSt = Number(custObj.balance).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                htmlName += '<br>Acc Open Date: ' + accObj.startdate;
                htmlName += '<br>Acc Bal: ' + balanceSt;
                if (custObj.payment != 0) {
                    var curPaySt = Number(custObj.payment).toLocaleString('en-US', {style: 'currency', currency: 'USD'});

                    htmlName += ' Paymment due: <font style= color:red>' + curPaySt + '</font>';
                }

            }
            if (accObj.type === INT_MUTUAL_FUND_ACCOUNT) { //INT_MUTUAL_FUND_ACCOUNT = 120;
                var total = accObj.investment + accObj.balance;
                var investSt = Number(accObj.investment).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                var balSt = Number(accObj.balance).toLocaleString('en-US', {style: 'currency', currency: 'USD'});

                var totSt = Number(total).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                htmlName += '<br>Acc Bal: ' + investSt + ' Cur Bal: ' + balSt + ' Total: ' + totSt;

            }

            htmlName += '</a></li>';
            $("#myid").append(htmlName);
        }



        $("ul[id*=myid] li").click(function () {
            return;
//            alert($(this).html()); // gets innerHTML of clicked li
//            alert($(this).text()); // gets text contents of clicked li
            var accId = $(this).attr('id');
            console.log(accId);
            if (accId == 0) {
//                alert(accId);
                return;
            }

            var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr, 'accId': accId};
            window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
            window.location.href = "accountst_1.html";
        });


        function functionConfirm(msg, myYes, myNo, myOk) {
            var confirmBox = $("#confirm");
            confirmBox.find(".message").text(msg);
            confirmBox.find(".yes,.no,.ok").unbind().click(function () {
                confirmBox.hide();
                window.localStorage.setItem(iisMsgSession, "");
            });
            confirmBox.find(".yes").click(myYes);
            confirmBox.find(".no").click(myNo);
            confirmBox.find(".ok").click(myOk);
            confirmBox.show();
        }

        function functionAlertConfirm(msg, myYes, myNo, myOk) {
            var confirmBox = $("#alertconfirm");
            confirmBox.find(".message").text(msg);
            confirmBox.find(".yes,.no,.ok").unbind().click(function () {
                confirmBox.hide();
                window.localStorage.setItem(iisMsgSession, "");
            });
            confirmBox.find(".yes").click(myYes);
            confirmBox.find(".no").click(myNo);
            confirmBox.find(".ok").click(myOk);
            confirmBox.show();
        }

// example        
//alert("AJAX request successfully completed");
//var jsonObj = JSON.parse(jsonStr);
//var jsonPretty = JSON.stringify(jsonObj, null, '\t');

    }
};
app.initialize();





