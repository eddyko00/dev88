
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

        var yearRpt = 0;
        if (iisWebObj.yearRpt != null) {
            yearRpt = iisWebObj.yearRpt;
        }
        var nameRpt = "income";
        if (iisWebObj.nameRpt != null) {
            nameRpt = iisWebObj.nameRpt;
        }
///////////////////////////
        $("#accheader").html("Accounting Report");


        var htmltrHeader = "";
        htmltrHeader += '<button type="submit" id="incomebtn" class="ui-btn ui-corner-all ui-shadow ui-btn-b ui-btn-icon-left"><small>Income Rpt</small></button>';
        htmltrHeader += '<button type="submit" id="balancebtn" class="ui-btn ui-corner-all ui-shadow ui-btn-b ui-btn-icon-left"><small>Balance Rpt</small></button>';
        htmltrHeader += '<button type="submit" id="depbtn" class="ui-btn ui-corner-all ui-shadow ui-btn-b ui-btn-icon-left"><small>Deprecation Rpt</small></button>';

        htmltrHeader += '<button type="submit" id="prevbtn" class="ui-btn ui-corner-all ui-shadow ui-btn-b ui-btn-icon-left"><small>prev year</small></button>';
        htmltrHeader += '<button type="submit" id="nextbtn" class="ui-btn ui-corner-all ui-shadow ui-btn-b ui-btn-icon-left"><small>next year</small></button>';
        htmltrHeader += '<button type="submit" id="yearendbtn" class="ui-btn ui-corner-all ui-shadow ui-btn-b ui-btn-icon-left"><small>YearEnd</small></button>';
        htmltrHeader += '<button type="submit" id="deletebtn" class="ui-btn ui-corner-all ui-shadow ui-btn-b ui-btn-icon-left"><small>*DeleteInYear*</small></button>';

        $("#trheader").html(htmltrHeader);


        if (reportObjStr !== "") {
            var reportObj = JSON.parse(reportObjStr);
            var entryList = reportObj.accTotalEntryBal;
            var beginDate = reportObj.begindisplay;
            var endDate = reportObj.enddisplay;
            var rangeSt = reportObj.name + ' - Yr:' + yearRpt + ' Date:' + beginDate + ' to ' + ' ' + endDate;
            $("#myid").html('<li ">' + rangeSt + '</li>');

//            var htmlhead = '<div class="ui-grid-d">';
//            htmlhead += '<div class="ui-block-a" style="width:20%"><strong>Date</strong></div>';
//            htmlhead += '<div class="ui-block-b" style="text-align: center;width:10%">Id</div>';
//            htmlhead += '<div class="ui-block-c" style="text-align: left;width:30%">Name</div>';
//            htmlhead += '<div class="ui-block-d" style="text-align: right">Income</div>';
//            htmlhead += '<div class="ui-block-e" style="text-align: right">Expense</div>';
//            htmlhead += '</div>';
            // Cash account is confusing, Revert it
            var htmlhead = '<div class="ui-grid-d">';
            htmlhead += '<div class="ui-block-a" style="width:20%"><strong>Date</strong></div>';
            htmlhead += '<div class="ui-block-b" style="text-align: center;width:10%">Id</div>';
            htmlhead += '<div class="ui-block-c" style="text-align: left;width:30%">Name</div>';
            htmlhead += '<div class="ui-block-d" style="text-align: right">Expense</div>';
            htmlhead += '<div class="ui-block-e" style="text-align: right">Income</div>';
            htmlhead += '</div>';
            $("#myid").append('<li id="0" >' + htmlhead + '</li>');
            for (i = 0; i < entryList.length; i++) {
                var entryObj = entryList[i];

                if (entryObj.id === -1) {
                    var htmlhead = '<div class="ui-grid-d">';
                    htmlhead += '<div class="ui-block-a" style="width:20%"><strong>' + entryObj.name + '</strong></div>';
                    htmlhead += '<div class="ui-block-b" style="text-align: center;width:10%"></div>';
                    htmlhead += '<div class="ui-block-c" style="text-align: left;width:30%"></div>';
                    htmlhead += '<div class="ui-block-d" style="text-align: right"></div>';
                    htmlhead += '<div class="ui-block-e" style="text-align: right"></div>';
                    htmlhead += '</div>';
                    $("#myid").append('<li id="0" >' + htmlhead + '</li>');
                    continue;
                }
                var entryId = i + 1;


                var htmlName = '<div class="ui-grid-d">';
                if (i == 0) {

                    htmlName += '<div class="ui-block-a" style="color:SteelBlue;width:20%"><strong>' + entryObj.dateSt + '</strong></div>';
                    htmlName += '<div class="ui-block-b" style="color:SteelBlue;text-align: center;width:10%">' + entryObj.id + '</div>';
                    htmlName += '<div class="ui-block-c" style="color:SteelBlue;text-align: left;width:30%"><small>' + entryObj.name + '</small></div>';
                    // Cash account is confusing, Revert it
                    var totStDebit = Number(entryObj.debit).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                    var totStCredit = Number(entryObj.credit).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                    if (entryObj.name === "cash") {
                        var tmp = totStCredit;
                        totStCredit = totStDebit;
                        totStDebit = tmp;
                    }

                    htmlName += '<div class="ui-block-d" style="color:SteelBlue;text-align: right">' + totStDebit + '</div>';
                    htmlName += '<div class="ui-block-e" style="color:SteelBlue;text-align: right">' + totStCredit + '</div>';
                    htmlName += '</div>';
                    $("#myid").append('<li id="' + entryId + ' "><a href="#">' + htmlName + '</a></li>');

//                    htmlhead = '<div class="ui-grid-d">';
//                    htmlhead += '<div class="ui-block-a" style="width:20%"><strong> </strong></div>';
//                    htmlhead += '<div class="ui-block-b" style="text-align: center;width:10%"> </div>';
//                    htmlhead += '<div class="ui-block-c" style="text-align: left;width:30%"> </div>';
//                    htmlhead += '<div class="ui-block-d" style="text-align: right">Debit</div>';
//                    htmlhead += '<div class="ui-block-e" style="text-align: right">Credit</div>';
//                    htmlhead += '</div>';
//                    $("#myid").append('<li>' + htmlhead + '</li>');
                    $("#myid").append('<li> </li>');
                    continue;
                }
                if (i + 1 === entryList.length) {

                    $("#myid").append('<li> </li>');
                }
                if (entryObj.name === "cash") {
                    if (nameRpt === "balance") {
                        ;
                    } else {
                        continue;
                    }
                }
                htmlName += '<div class="ui-block-a" style="width:20%"><strong>' + entryObj.dateSt + '</strong></div>';
                htmlName += '<div class="ui-block-b" style="text-align: center;width:10%">' + entryObj.id + '</div>';
                htmlName += '<div class="ui-block-c" style="text-align: left;width:30%"><small>' + entryObj.name + '</small></div>';

                if (nameRpt === "balance") {
                    var totSt = Number(entryObj.total).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                    htmlName += '<div class="ui-block-d" style="text-align: right"></div>';
                    htmlName += '<div class="ui-block-e" style="text-align: right">' + totSt + '</div>';
                    htmlName += '</div>';
                } else {
                    var totSt = Number(entryObj.debit).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                    htmlName += '<div class="ui-block-d" style="text-align: right">' + totSt + '</div>';
                    totSt = Number(entryObj.credit).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                    htmlName += '<div class="ui-block-e" style="text-align: right">' + totSt + '</div>';
                    htmlName += '</div>';
                }
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

                var htmlName = '<div class="ui-grid-d">';
                htmlName += '<div class="ui-block-a" style="width:20%"><strong>' + entryObj.dateSt + '</strong></div>';
                htmlName += '<div class="ui-block-b" style="text-align: center;width:10%">' + entryObj.id + '</div>';
                htmlName += '<div class="ui-block-c" style="text-align: left;width:30%"><small>' + entryObj.name + '</small></div>';


                var totStDebit = Number(entryObj.debit).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                var totStCredit = Number(entryObj.credit).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                // Cash account is confusing, Revert it
                if (entryObj.name === "cash") {
                    var tmp = totStCredit;
                    totStCredit = totStDebit;
                    totStDebit = tmp;
                }
                htmlName += '<div class="ui-block-d" style="text-align: right">' + totStDebit + '</div>';
                htmlName += '<div class="ui-block-e" style="text-align: right">' + totStCredit + '</div>';
                htmlName += '</div>';
                htmlName += '<p>' + entryObj.comment;
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
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id + "/accounting/report?name=" + name + "&year=" + yearRpt + "&namerpt=" + nameRpt,
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
                        'reportObjStr': reportObjStr, 'entryObjStr': entryObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

                    window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

                    window.location.href = "accountadmrp.html";
                }
            });

        });


        //utility
        $("#costsubmit").click(function () {
            var costamount = document.getElementById("costamount").value;
            if (costamount === "") {
                window.location.href = "accountadmrp.html";
                return;
            }
            var costyr = document.getElementById("costyr").value;

            var comment = document.getElementById("costcomm").value;
            var payment = costamount;
            ///cust/{username}/uisys/{custid}/accounting/utility?payment=&curyear=&reason=&comment=
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id
                        + "/accounting/utility?payment=" + payment + "&curyear=" + costyr + "&comment=" + comment,
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
                    'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

                window.location.href = "accountadmrp_1.html";
            }
        });

        // customer withdraw done in admin page
//        $("#withsubmit").click(function () {
//            var examount = document.getElementById("withamount").value;
//            if (examount === "") {
//                window.location.href = "accountadmrp.html";
//                return;
//            }
//            var comment = document.getElementById("withcomm").value;
//
//            var payment = examount;
//            if (yearRpt !== 0) {
//                if (confirm('Do you want to add transaction in year ' + yearRpt + '?')) {
//                    ;
//                } else {
//                    return;
//                }
//            }
//            ///cust/{username}/uisys/{custid}/accounting/update?payment=&balance=&reason=&comment=
//            $.ajax({
//                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id
//                        + "/accounting/update?payment=" + payment + "&year=" + yearRpt + "&reason=E_USER_WITHDRAWAL" + "&comment=" + comment,
//                crossDomain: true,
//                cache: false,
//                success: handleResult
//            }); // use promises
//
//            // add cordova progress indicator https://www.npmjs.com/package/cordova-plugin-progress-indicator
//            function handleResult(result) {
//                console.log(result);
//                var resultmsg = "Accounting Update result: " + result;
//                if (result == '1') {
//                    resultmsg += "  - success";
//                } else {
//                    resultmsg += "  - fail";
//                }
//                alert(resultmsg);
//
//
//                var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
//                    'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};
//
//                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
//
//                window.location.href = "accountadmrp_1.html";
//            }
//        });

        $("#exsubmit").click(function () {
            var examount = document.getElementById("examount").value;
            if (examount === "") {
                window.location.href = "accountadmrp.html";
                return;
            }
            var comment = document.getElementById("excomm").value;
            var rate = document.getElementById("exrate").value;
            if ((rate === "") || (rate === "50") || (rate === "100")) {
                ;
            } else {
                var resultmsg = "Accounting Update error: Only accept 100% or 50% ";
                alert(resultmsg);
                window.location.href = "accountadmrp.html";
                return;
            }
            var payment = examount;
            if (yearRpt !== 0) {
                if (confirm('Do you want to add transaction in year ' + yearRpt + '?')) {
                    ;
                } else {
                    return;
                }
            }
            ///cust/{username}/uisys/{custid}/accounting/update?payment=&balance=&reason=&comment=
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id
                        + "/accounting/update?payment=" + payment + "&year=" + yearRpt + "&rate=" + rate + "&comment=" + comment,
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
                    'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

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
            if (yearRpt !== 0) {
                if (confirm('Do you want to add transaction in year ' + yearRpt + '?')) {
                    ;
                } else {
                    return;
                }
            }
            ///cust/{username}/uisys/{custid}/accounting/update?payment=&balance=&reason=&comment=
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id
                        + "/accounting/update?balance=" + balance + "&year=" + yearRpt + "&comment=" + comment,
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
                    'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

                window.location.href = "accountadmrp_1.html";
            }
        });

        $("#depsubmit").click(function () {
            var depamount = document.getElementById("depamount").value;
            if (depamount === "") {
                window.location.href = "accountadmrp.html";
                return;
            }
            var deprate = document.getElementById("deprate").value;
            if (deprate === "") {
                window.location.href = "accountadmrp.html";
                return;
            }
            var comment = document.getElementById("depcomm").value;
            var payment = depamount;
            ///cust/{username}/uisys/{custid}/accounting/deprecation?payment=&rate=&reason=&comment="
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id
                        + "/accounting/deprecation?payment=" + payment + "&rate=" + deprate + "&comment=" + comment,
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
                    'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

                window.location.href = "accountadmrp_1.html";
            }
        });

        //"/cust/{username}/uisys/{custid}/accounting/tax?payment=&reason=&comment=
        $("#taxsubmit").click(function () {
            var taxamount = document.getElementById("taxamount").value;
            if (taxamount === "") {
                window.location.href = "accountadmrp.html";
                return;
            }
            var comment = document.getElementById("taxcomm").value;

            var payment = taxamount;
            if (yearRpt !== 0) {
                if (confirm('Do you want to add transaction in year ' + yearRpt + '?')) {
                    ;
                } else {
                    return;
                }
            }
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id
                        + "/accounting/tax?payment=" + payment + "&year=" + yearRpt + "&comment=" + comment,
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
                    'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

                window.location.href = "accountadmrp_1.html";
            }
        });

        $("#casubmit").click(function () {
            var taxamount = document.getElementById("caamount").value;
            if (taxamount === "") {
                window.location.href = "accountadmrp.html";
                return;
            }
            var comment = document.getElementById("cacomm").value;

            var payment = taxamount;
            if (yearRpt !== 0) {
                if (confirm('Do you want to add transaction in year ' + yearRpt + '?')) {
                    ;
                } else {
                    return;
                }
            }
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id
                        + "/accounting/cash?payment=" + payment + "&year=" + yearRpt + "&comment=" + comment,
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
                    'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

                window.location.href = "accountadmrp_1.html";
            }
        });


        $("#nextbtn").click(function () {
            yearRpt = yearRpt + 1;
            var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
                'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

            window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

            window.location.href = "accountadmrp_1.html";
        });

        $("#prevbtn").click(function () {
            yearRpt = yearRpt - 1;
            var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
                'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

            window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

            window.location.href = "accountadmrp_1.html";
        });


        $("#incomebtn").click(function () {

            nameRpt = "income";
            var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
                'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

            window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

            window.location.href = "accountadmrp_1.html";
        });

        $("#balancebtn").click(function () {
            nameRpt = "balance";
            var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
                'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

            window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

            window.location.href = "accountadmrp_1.html";
        });

        $("#depbtn").click(function () {
            nameRpt = "deprecation";
            var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
                'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

            window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

            window.location.href = "accountadmrp_1.html";
        });

        $("#yearendbtn").click(function () {
            if (confirm('Do you want to proceed year end closing in year ' + yearRpt + '?')) {
                ;
            } else {
                return;
            }
            if (confirm('Are you really sure?')) {
                ;
            } else {
                return;
            }
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id
                        + "/accounting/yearend?year=" + yearRpt,
                crossDomain: true,
                cache: false,
                success: handleResult
            }); // use promises

            // add cordova progress indicator https://www.npmjs.com/package/cordova-plugin-progress-indicator
            function handleResult(result) {
                console.log(result);
                var resultmsg = "Accounting delete result: " + result;
                if (result == '1') {
                    resultmsg += "  - success";
                } else {
                    resultmsg += "  - fail";
                }
                alert(resultmsg);


                var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
                    'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

                window.location.href = "accountadmrp_1.html";
            }
        });

        $("#deletebtn").click(function () {
            if (confirm('Do you want to delete all accounting in year ' + yearRpt + '?')) {
                ;
            } else {
                return;
            }
            if (confirm('Are you really sure?')) {
                ;
            } else {
                return;
            }
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/uisys/" + custObj.id
                        + "/accounting/removeaccounting?year=" + yearRpt,
                crossDomain: true,
                cache: false,
                success: handleResult
            }); // use promises

            // add cordova progress indicator https://www.npmjs.com/package/cordova-plugin-progress-indicator
            function handleResult(result) {
                console.log(result);
                var resultmsg = "Accounting delete result: " + result;
                if (result == '1') {
                    resultmsg += "  - success";
                } else {
                    resultmsg += "  - fail";
                }
                alert(resultmsg);


                var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr,
                    'reportObjStr': reportObjStr, 'yearRpt': yearRpt, 'nameRpt': nameRpt};

                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));

                window.location.href = "accountadmrp_1.html";
            }
        });

// 
// example        
//alert("AJAX request successfully completed");
//var jsonObj = JSON.parse(jsonStr);
//var jsonPretty = JSON.stringify(jsonObj, null, '\t');

    }
};
app.initialize();





