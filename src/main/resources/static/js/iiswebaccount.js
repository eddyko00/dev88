
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


        var htmlAdmin = '<button id="configbtn"  >Configuration</button>';
        htmlAdmin += '<button id="invoicebtn"  >Billing Invoice</button>';

        $("#adminid").html(htmlAdmin);
        if (custObj.type == 99) {
            var htmlAdmin = '<br><br><button id="sysbtn" >System Status</button>';
            htmlAdmin += '<button id="admsgbtn"  >Admin Msg</button>';

            $("#adminid").append(htmlAdmin);
        }

        $("#accheader").html("Customer Account");

        $("#myid").html(" "); //clear the field
        for (i = 0; i < accObjList.length; i++) {
            var accObj = accObjList[i];
            console.log(accObj);
            var accName = accObj.accountname;
            var accId = accObj.id;

            var htmlName = '';
            htmlName += '<li id="' + accId + '"><a href="#">';
            htmlName += '<br>Account: ' + accName;
            if (accObj.type == 110) { //INT_TRADING_ACCOUNT           
                var pp = "Basic Plan - Max 5 stocks";
                if (custObj.substatus == 0) {
                    pp = "Basic Plan - Max 5 stocks";
                } else if (custObj.substatus == 10) {
                    pp = "Premium Plan - Max 10 stocks";
                } else if (custObj.substatus == 20) {
                    pp = "Deluxe Plan - Max 20 stocks";
                }

                htmlName += '<br>Plan: ' + pp;

                htmlName += '<br>Date: ' + accObj.startdate;
                htmlName += '<br>Bal: $' + custObj.balance.toFixed(2)
                        + ' Amount due: $' + custObj.payment.toFixed(2);

            }
            if (accObj.type == 120) { //INT_MUTUAL_FUND_ACCOUNT = 120;
                var total = accObj.investment + accObj.balance;
                htmlName += '<br>Past: $' + accObj.investment.toFixed(2)
                        + ' Cur: $' + accObj.balance.toFixed(2)
                        + '   Total: $' + total.toFixed(2);

            }

            htmlName += '</a></li>';
            $("#myid").append(htmlName);
        }



        $("ul[id*=myid] li").click(function () {
//            alert($(this).html()); // gets innerHTML of clicked li
//            alert($(this).text()); // gets text contents of clicked li
            var accId = $(this).attr('id');
            console.log(accId);
            if (accId == 0) {
//                alert(accId);
                return;
            }

            var iisWebObj = {'custObjStr': custObjStr, 'accObjListStr': accObjListStr, 'accId': accId};
            window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
            window.location.href = "accountst_1.html";
        });


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
            var iisWebObj = {'custObjStr': custObjStr, 'accObjListStr': accObjListStr};
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
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/acc/" + accObj.id + "/comm",

                crossDomain: true,
                cache: false,
                success: function (resultCommAdmObjList) {
                    console.log(resultCommAdmObjList);
                    if (resultCommAdmObjList !== "") {
                        ;
                    } else {
                        window.location.href = "#page-index";
                        return;
                    }
                    var commAdmObjListStr = JSON.stringify(resultCommAdmObjList, null, '\t');
                    console.log(commAdmObjListStr);
                    if (commAdmObjListStr !== "") {
                        var commAdmObjList = JSON.parse(commAdmObjListStr);

                        var htmlhead = '<div class="ui-grid-b">';
                        htmlhead += '<div class="ui-block-a" style="width:30%"><strong>Date</strong></div>';
                        htmlhead += '<div class="ui-block-b" style="width:5%"></div>';
                        htmlhead += '<div class="ui-block-c">Msg</div>';
                        htmlhead += '</div>';

                        $("#admmsgid").html('<li id="0" >' + htmlhead + '</li>');

                        for (i = 0; i < commAdmObjList.length; i++) {
                            var commObj = commAdmObjList[i];
                            var commId = commObj.id;

                            var htmlName = '<div class="ui-grid-b">';
                            htmlName += '<div class="ui-block-a" style="width:30%"><strong>' + commObj.updatedatedisplay + '</strong></div>';
                            htmlName += '<div class="ui-block-b" style="width:5%"> </div>';
                            htmlName += '<div class="ui-block-c">id:' + commId + " " + commObj.data + '</div>';
                            htmlName += '</div>';

                            $("#admmsgid").append('<li id="' + commId + '" >' + htmlName + '</li>');

                        }
                        window.location.href = "#page-admmsg";
                        return;
                    }

                }
            });
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
                alert("Not supproted feature for GUEST accont");
                return;
            }
        });
        $("#invoicebtn").click(function () {
            if (custObj.username.toUpperCase() === "GUEST") {
                alert("Not supproted feature for GUEST accont");
                return;
            }
        });


// example        
//alert("AJAX request successfully completed");
//var jsonObj = JSON.parse(jsonStr);
//var jsonPretty = JSON.stringify(jsonObj, null, '\t');

    }
};
app.initialize();





