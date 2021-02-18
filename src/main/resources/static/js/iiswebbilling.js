
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

        var billObjListStr = iisWebObj.billObjListStr;
        var billObjList = JSON.parse(billObjListStr);


        $("#accheader").html("Account Billing");

        $("#myid").html(" "); //clear the field
        for (i = 0; i < billObjList.length; i++) {
            var billObj = billObjList[i];
            console.log(billObj);
            var billName = billObj.name;
            var billId = billObj.id;

            var htmlName = '';
            htmlName += '<br>Billing id: (' + billId + ') Account: ' + billName;

            htmlName += '<br>Due date: ' + billObj.updatedatedisplay
            var statusSt = 'NA';
            if (billObj.status === 2) {
                statusSt = 'Amount due';
            }
            if (billObj.status === 5) {
                statusSt = 'Amount paid';
            }

            var curPaySt = Number(billObj.payment).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
            htmlName += '<br>Status:' + statusSt + ' invoice: ' + curPaySt;

            $("#myid").append('<li id="' + billId + '" >' + htmlName + '</li>');

        }



//        $("ul[id*=myid] li").click(function () {
////            alert($(this).html()); // gets innerHTML of clicked li
////            alert($(this).text()); // gets text contents of clicked li
//            var accId = $(this).attr('id');
//            console.log(accId);
//            if (accId == 0) {
////                alert(accId);
//                return;
//            }
//
//            var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr, 'accId': accId};
//            window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
//            window.location.href = "accountst_1.html";
//        });


        $("#clrbtn").click(function () {
            var accObjList = JSON.parse(accObjListStr);

            var accObj = null;
            for (i = 0; i < accObjList.length; i++) {
                var accObjTmp = accObjList[i];
                if (accObjTmp.type == 110) { //INT_TRADING_ACCOUNT
                    accObj = accObjTmp;
                    break;
                }
            }
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/acc/" + accObj.id + "/comm/remove",
                crossDomain: true,
                cache: false,
                beforeSend: function () {
                    $("#loader").show();
                },

                success: function (result) {
                    console.log(result);
                    window.location.href = "account_1.html";
                }
            });

        });


        $("#sysbtn").click(function () {
            var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr};
            window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
            window.location.href = "accountstatus_1.html";
            return;
        });


        $("#admsgbtn").click(function () {
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
                window.location.href = "#page-index";
            }
            var CustNListStr = "";
            var CustNListCnt = 0;
            var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
                'CustNListStr': CustNListStr, 'CustNListCnt': CustNListCnt};
            window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
            window.location.href = "accountadm_1.html";
//            $.ajax({
//                url: iisurl + "/cust/" + custObj.username + "/acc/" + accObj.id + "/comm",
//
//                crossDomain: true,
//                cache: false,
//                success: function (resultCommAdmObjList) {
//                    console.log(resultCommAdmObjList);
//                    if (resultCommAdmObjList !== "") {
//                        ;
//                    } else {
//                        window.location.href = "#page-index";
//                        return;
//                    }
//                    var commAdmObjListStr = JSON.stringify(resultCommAdmObjList, null, '\t');
//                    console.log(commAdmObjListStr);
//                    if (commAdmObjListStr !== "") {
//                        var commAdmObjList = JSON.parse(commAdmObjListStr);
//
//                        var htmlhead = '<div class="ui-grid-b">';
//                        htmlhead += '<div class="ui-block-a" style="width:30%"><strong>Date</strong></div>';
//                        htmlhead += '<div class="ui-block-b" style="width:5%"></div>';
//                        htmlhead += '<div class="ui-block-c">Msg</div>';
//                        htmlhead += '</div>';
//
//                        $("#admmsgid").html('<li id="0" >' + htmlhead + '</li>');
//
//                        for (i = 0; i < commAdmObjList.length; i++) {
//                            var commObj = commAdmObjList[i];
//                            var commId = commObj.id;
//
//                            var htmlName = '<div class="ui-grid-b">';
//                            htmlName += '<div class="ui-block-a" style="width:30%"><strong>' + commObj.updatedatedisplay + '</strong></div>';
//                            htmlName += '<div class="ui-block-b" style="width:5%"> </div>';
//                            htmlName += '<div class="ui-block-c">id:' + commId + " " + commObj.data + '</div>';
//                            htmlName += '</div>';
//
//                            $("#admmsgid").append('<li id="' + commId + '" >' + htmlName + '</li>');
//
//                        }
//                        window.location.href = "#page-admmsg";
//                        return;
//                    }
//
//                }
//            });
        });

        $("#admclrbtn").click(function () {

            var accObjList = JSON.parse(accObjListStr);

            var accObj = null;
            for (i = 0; i < accObjList.length; i++) {
                var accObjTmp = accObjList[i];
                if (accObjTmp.type == 140) { //INT_ADMIN_ACCOUNT = 140;
                    accObj = accObjTmp;
                    break;
                }
            }
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/acc/" + accObj.id + "/comm/remove",
                crossDomain: true,
                cache: false,
                beforeSend: function () {
                    $("#loader").show();
                },

                success: function (result) {
                    console.log(result);
                    window.location.href = "account_1.html";
                }
            });
        });

        $("#configbtn").click(function () {
            if (custObj.username.toUpperCase() === "GUEST") {
                msgObjStr = "This feature does not allow for GUEST account";
                window.localStorage.setItem(iisMsgSession, msgObjStr);
                window.location.href = "account.html";
            }
        });

        $("#invoicebtn").click(function () {
            if (custObj.username.toUpperCase() === "GUEST") {
                msgObjStr = "This feature does not allow for GUEST account";
                window.localStorage.setItem(iisMsgSession, msgObjStr);
                window.location.href = "account.html";
            }
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





