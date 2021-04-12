
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

        var reportObjStr = iisWebObj.reportObjStr;
        var entryObjStr = iisWebObj.entryObjStr;
///////////////////////////
        $("#accheader").html("Accounting Report");




        if (reportObjStr !== "") {
            var reportObj = JSON.parse(reportObjStr);
            var entryList = reportObj.accTotalEntryBal;
            var beginDate = reportObj.begindisplay;
            var endDate = reportObj.enddisplay;
            var rangeSt = 'Begin Date:' + beginDate + ' - ' + 'End Date:' + endDate;
            $("#myid").html('<li ">' + rangeSt + '</li>');

            var htmlhead = '<div class="ui-grid-c">';
            htmlhead += '<div class="ui-block-a" style="width:20%"><strong>Date</strong></div>';
            htmlhead += '<div class="ui-block-b" style="width:10%">Id</div>';
            htmlhead += '<div class="ui-block-c" style="width:40%">Name</div>';
            htmlhead += '<div class="ui-block-d" style="width:30%">Amount</div>';
            htmlhead += '</div>';

            $("#myid").append('<li id="0" >' + htmlhead + '</li>');
            for (i = 0; i < entryList.length; i++) {
                var entryObj = entryList[i];
                var entryId = i + 1;

                var htmlName = '<div class="ui-grid-c">';
                htmlName += '<div class="ui-block-a" style="width:20%"><strong>' + entryObj.dateSt + '</strong></div>';
                htmlName += '<div class="ui-block-b" style="width:10%">' + entryObj.id + '</div>';
                htmlName += '<div class="ui-block-c" style="width:40%">' + entryObj.name + '</div>';

                var totSt = Number(entryObj.amount).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                htmlName += '<div class="ui-block-d" style="width:30%">' + totSt + '</div>';
                htmlName += '</div>';
                $("#myid").append('<li id="' + entryId + ' "><a href="#">' + htmlName + '</a></li>');
            }
        }

        htmlhead = 'Accounting Entry Listing';
        $("#entryid").html('<li id="0" >' + htmlhead + '</li>');

        if (entryObjStr !== "") {
            var entryObjList = JSON.parse(entryObjStr);
            var entryList = entryObjList.accEntryBal;

            for (i = 0; i < entryList.length; i++) {
                var entryObj = entryList[i];
                var entryId = i + 1;

                var htmlName = '<div class="ui-grid-c">';
                htmlName += '<div class="ui-block-a" style="width:20%"><strong>' + entryObj.dateSt + '</strong></div>';
                htmlName += '<div class="ui-block-b" style="width:10%">' + entryObj.id + '</div>';
                htmlName += '<div class="ui-block-c" style="width:40%">' + entryObj.name + '</div>';

                var totSt = Number(entryObj.amount).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                htmlName += '<div class="ui-block-d" style="width:30%">' + totSt + '</div>';
                htmlName += '</div>';
                $("#entryid").append('<li id="' + entryId + ' ">' + htmlName + '</li>');
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
            var entryList = reportObj.accTotalEntryBal;
            var entryObj = entryList[Id - 1];
            var name = entryObj.name;

            ///cust/{username}/uisys/{custid}/accounting/report?year=");
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id + "/accounting/report?name=" + name,
                crossDomain: true,
                cache: false,
                beforeSend: function () {
                    $("#loader").show();
                },
                error: function () {
                    alert('Network failure. Please try again later.');
                    window.location.href = "index.html";
                },
                success: function (resultRptObj) {
                    console.log(resultRptObj);

                    var entryObjStr = JSON.stringify(resultRptObj, null, '\t');
                    var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
                        'reportObjStr': reportObjStr, 'entryObjStr': entryObjStr};

                    window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

                    window.location.href = "accountadmrp.html";
                }
            });

        });



        $("#costsubmit").click(function () {
            var costamount = document.getElementById("costamount").value;
            if (costamount === "") {
                window.location.href = "accountadmrp.html";
                return;
            }
            var comment = document.getElementById("costcomm").value;
            var payment = costamount;
            ///cust/{username}/uisys/{custid}/accounting/update?payment=&balance=&reason=&comment=
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id
                        + "/accounting/update?payment=" + payment + "&reason=E_COST_SERVICE&comment=" + comment,
                crossDomain: true,
                cache: false,
                success: handleResult
            }); // use promises

            // add cordova progress indicator https://www.npmjs.com/package/cordova-plugin-progress-indicator
            function handleResult(result) {
                console.log(result);
                var resultmsg = "Accounting Update result: " + result;
                if (result == '1') {
                    resultmsg += "  - success";
                } else {
                    resultmsg += "  - fail";
                }
                alert(resultmsg);


                var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
                    'reportObjStr': reportObjStr};

                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

                window.location.href = "accountadmrp.html";
            }
        });

        $("#exsubmit").click(function () {
            var examount = document.getElementById("examount").value;
            if (examount === "") {
                window.location.href = "accountadmrp.html";
                return;
            }
            var comment = document.getElementById("excomm").value;
            var payment = examount;
            ///cust/{username}/uisys/{custid}/accounting/update?payment=&balance=&reason=&comment=
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id
                        + "/accounting/update?payment=" + payment + "&comment=" + comment,
                crossDomain: true,
                cache: false,
                success: handleResult
            }); // use promises

            // add cordova progress indicator https://www.npmjs.com/package/cordova-plugin-progress-indicator
            function handleResult(result) {
                console.log(result);
                var resultmsg = "Accounting Update result: " + result;
                if (result == '1') {
                    resultmsg += "  - success";
                } else {
                    resultmsg += "  - fail";
                }
                alert(resultmsg);


                var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
                    'reportObjStr': reportObjStr};

                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

                window.location.href = "accountadmrp_1.html";
            }
        });

        $("#revsubmit").click(function () {
            var revamount = document.getElementById("revamount").value;
            if (revamount === "") {
                window.location.href = "accountadmrp.html";
                return;
            }
            var comment = document.getElementById("revcomm").value;
            var balance = revamount;
            ///cust/{username}/uisys/{custid}/accounting/update?payment=&balance=&reason=&comment=
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id
                        + "/accounting/update?balance=" + balance + "&comment=" + comment,
                crossDomain: true,
                cache: false,
                success: handleResult
            }); // use promises

            // add cordova progress indicator https://www.npmjs.com/package/cordova-plugin-progress-indicator
            function handleResult(result) {
                console.log(result);
                var resultmsg = "Accounting Update result: " + result;
                if (result == '1') {
                    resultmsg += "  - success";
                } else {
                    resultmsg += "  - fail";
                }
                alert(resultmsg);


                var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
                    'reportObjStr': reportObjStr};

                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

                window.location.href = "accountadmrp_1.html";
            }
        });


// example        
//alert("AJAX request successfully completed");
//var jsonObj = JSON.parse(jsonStr);
//var jsonPretty = JSON.stringify(jsonObj, null, '\t');

    }
};
app.initialize();





