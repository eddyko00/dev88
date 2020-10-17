
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
        var trObjListStr = iisWebObj.trObjListStr;
        var trObjList = JSON.parse(trObjListStr);
        var trObjacc = null;
        var percentSt = "";
        var close = 0;
        var preClose = 0;
        if (stockObj.afstockInfo != null) {
            close = stockObj.afstockInfo.fclose;
            preClose = stockObj.prevClose;
            var percent = 100 * (close - preClose) / preClose;
            percentSt = percent.toFixed(2) + '%';
        }

        var stStr = 'Trading Model Listing<br>';
        var stStatus = "";
        if (stockObj.substatus == 12) { //ConstantKey.STOCK_SPLIT STOCK_DETLA = 12
            stStatus = "St: L Detla";
        }
        if (stockObj.substatus == 10) { //ConstantKey.STOCK_SPLIT STOCK_SPLIT = 10
            stStatus = "St: Split";
        }
        if (stockObj.substatus == 2) { //INITIAL = 2;
            stStatus = "St: Init";
        }
        if (stockObj.substatus == 0) { //INITIAL = 2;
            stStatus = "St: Ready";
        }
        stStr += stockObj.stockname + '<br>' + stockObj.updateDateD + " " + stStatus + '<br>' +
                'Pre Cl:' + preClose + '  Close:' + close + '  Per:' + percentSt
        $("#0").html('<h1>' + stStr + '</h1>');

        $("#accheader").html(" " + accObj.accountname + " " + stockObj.symbol);


        for (i = 0; i < trObjList.length; i++) {
            var trObj = trObjList[i];
            console.log(trObj);
            var nameId = trObj.id;
            if (custObj.username.toUpperCase() === "GUEST") {
                if (trObj.trname === "TR_MV") {
                    ;
                } else if (trObj.trname === "TR_MACD") {
                    ;
                } else if (trObj.trname === "TR_NN1") {
                    ;
                } else if (trObj.trname === "TR_NN2") {
                    ;
                } else if (trObj.trname === "TR_ACC") {
                    trObjacc = trObj;
                } else {
                    continue;
                }
            } else {
                if (trObj.trname === "TR_MV") {
                    ;
                } else if (trObj.trname === "TR_MACD") {
                    ;
                } else if (trObj.trname === "TR_NN1") {
                    ;
                } else if (trObj.trname === "TR_NN2") {
                    ;
                } else if (trObj.trname === "TR_NN3") {
                    ;
                } else if (trObj.trname === "TR_ACC") {
                    trObjacc = trObj;
                } else {
                    continue;
                }
            }


//https://demos.jquerymobile.com/1.1.2/docs/content/content-grids.html
            var htmlName = '<div class="ui-grid-b">';
            var dispName = trObj.trname;
            if (trObj.trname === "TR_ACC") {
                var status = trObj.status;
                if (status == 2) { //int PENDING = 2;
                    dispName = "Deleting"
                } else {
                    dispName = "ACCOUNT"
                }
            }

            htmlName += '<div class="ui-block-a" ><strong>' + dispName + '</strong></div>';
            var sharebalance = 0;
            var signal = "B";
            if (trObj.trsignal == S_BUY) {
                sharebalance = trObj.longamount;
                signal = "B";
            } else if (trObj.trsignal == S_SELL) {
                signal = "S";
                sharebalance = trObj.shortamount;
            } else {
                signal = "E";
            }
            htmlName += '<div class="ui-block-b" style="width:20%">:' + signal + '</div>';
            var total = trObj.balance + sharebalance;
            total = total - trObj.investment;
            var totalSt = Number(total).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
//            var totalSt = total.toFixed(2);
            htmlName += '<div class="ui-block-c">Profit: ' + totalSt + '</div>';
            htmlName += '</div>';

//            var trStr = '  L:' + trObj.longamount + ' LS:' + trObj.longshare + ' S:' + trObj.shortamount + ' SS:' + trObj.shortshare
//            htmlName += '<h3>' + trStr + '</h3>';

            var htmlBtn = '<div id="myidbtn"  data-role="controlgroup" data-type="horizontal" data-theme="a" style="font-size:0.7em; margin-left:auto; margin-right:auto;width:100%; ">';
            htmlBtn += '<a href="#" id="' + nameId + '" type="graph"  value="' + trObj.trname + '" data-icon="myicongraph" data-role="button" data-theme="a"></a>';
            htmlBtn += '<a href="#" id="' + nameId + '" type="table"  value="' + trObj.trname + '" data-icon="myicontable" data-role="button" data-theme="a"></a>';
            if (trObj.trname === "TR_ACC") {
                if (trObj.linktradingruleid == 0) {
                    if (trObj.trsignal == S_BUY) {
                        htmlBtn += '<a href="#" id="' + nameId + '" type=""  value="' + trObj.trname + '" data-icon="" data-role="button" data-theme="a"></a>';
                    } else {
                        htmlBtn += '<a href="#" id="' + nameId + '" type="buy"  value="' + trObj.trname + '" data-icon="myiconbuy" data-role="button" data-theme="a"></a>';
                    }
                    if (trObj.trsignal == S_SELL) {
                        htmlBtn += '<a href="#" id="' + nameId + '" type=""  value="' + trObj.trname + '" data-icon="" data-role="button" data-theme="a"></a>';
                    } else {
                        htmlBtn += '<a href="#" id="' + nameId + '" type="sell"  value="' + trObj.trname + '" data-icon="myiconsell" data-role="button" data-theme="a"></a>';
                    }
                    htmlBtn += '<a href="#" id="' + nameId + '" type="exit"  value="' + trObj.trname + '" data-icon="myiconexit" data-role="button" data-theme="a"></a>';

                }
            }
            htmlBtn += '</div>';

            htmlName += htmlBtn;
            if (trObj.trname === "TR_NN1") {
                htmlName += 'Auto Trading Signal using AI Model';
            } else if (trObj.trname === "TR_NN2") {
                htmlName += 'Auto Trading Signal using AI Model';
            } else if (trObj.trname === "TR_ACC") {

                var link = trObj.linktradingruleid;
                var trObjlink = null;
                for (j = 0; j < trObjList.length; j++) {
                    var trObjtmp = trObjList[j];
                    if (link == trObjtmp.type) {
                        trObjlink = trObjtmp;
                        break;
                    }
                }
                if (trObjlink != null) {
                    if (trObjlink.type == 0) {
                        htmlName += 'Manual buy sell Transaction';
                    } else {
                        htmlName += 'Auto Trading Signal from ' + trObjlink.trname;
                    }
                }
            }

            $("#myid").append('<li id="' + nameId + '"><a href="#">' + htmlName + '</a></li>');

        }

        var htmlName = '<div class="ui-grid-b">';
        htmlName += '<div class="ui-block-a" ></strong></div>';
        htmlName += '<div class="ui-block-b" style="width:20%"></div>';

        htmlName += '<div class="ui-block-c"></div>';
        htmlName += '</div>';
        htmlName = '<button id="configbtn"  >Configuration</button>';

        $("#myid").append('<li id="0">' + htmlName + '</li>');

//
        var buttonGraph = false;
//        
        $("[id*=myidbtn] a").click(function () {
            buttonGraph = true;
            var type = $(this).attr('type');
            var trname = $(this).attr('value');

            ////////////////////////////////
            if (type === "buy") {
                if (trname != null) {
                    var symbol = stockObj.symbol;
                    symbol = symbol.replace(".", "_");
//"/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/tran/{signal}/order
                    var sig = S_BUY;
                    var trObj;
                    for (j = 0; j < trObjList.length; j++) {
                        var trObjtmp = trObjList[j];
                        if (trObjtmp.trname === trname) {
                            trObj = trObjtmp;
                            break;
                        }
                    }
                    if (trObj.trsignal == sig) {
                        window.location.href = "#page_index";
                    }
                    if (confirm('Do you want to Buy Transaction?')) {
                        ;
                    } else {
                        window.location.href = "#page_index";
                        return;
                    }
                    var urlSt = iisurl + "cust/" + custObj.username + "/acc/" + accId + "/st/" + symbol + "/tr/" + trname + "/tran/" + sig + "/order";
                    console.log(urlSt);
                    $.ajax({
                        url: urlSt,
                        crossDomain: true,
                        cache: false,
                        beforeSend: function () {
                            $("#loader").show();
                        },
                        success: function (result) {
                            console.log(result);
                            window.location.href = "accountsttr_1.html";
                        }
                    });
                }
            }
            if (type === "sell") {
                if (trname !== null) {
                    var symbol = stockObj.symbol;
                    symbol = symbol.replace(".", "_");
//"/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/tran/{signal}/order                    
                    var sig = S_SELL;
                    var trObj;
                    for (j = 0; j < trObjList.length; j++) {
                        var trObjtmp = trObjList[j];
                        if (trObjtmp.trname === trname) {
                            trObj = trObjtmp;
                            break;
                        }
                    }
                    if (trObj.trsignal == sig) {
                        window.location.href = "#page_index";
                    }
                    if (confirm('Do you want to Sell Transaction?')) {
                        ;
                    } else {
                        window.location.href = "#page_index";
                        return;
                    }
                    var urlSt = iisurl + "cust/" + custObj.username + "/acc/" + accId + "/st/" + symbol + "/tr/" + trname + "/tran/" + sig + "/order";
                    console.log(urlSt);
                    $.ajax({
                        url: urlSt,
                        crossDomain: true,
                        cache: false,
                        beforeSend: function () {
                            $("#loader").show();
                        },
                        success: function (result) {
                            console.log(result);
                            window.location.href = "accountsttr_1.html";
                        }
                    });
                }
            }

            if (type === "exit") {
                if (trname != null) {
                    var symbol = stockObj.symbol;
                    symbol = symbol.replace(".", "_");
//"/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/tran/{signal}/order
                    var sig = S_NEUTRAL;
                    var trObj;
                    for (j = 0; j < trObjList.length; j++) {
                        var trObjtmp = trObjList[j];
                        if (trObjtmp.trname === trname) {
                            trObj = trObjtmp;
                            break;
                        }
                    }
                    if (trObj.trsignal == sig) {
                        window.location.href = "#page_index";
                    }
                    if (confirm('Do you want to Exit Buy or Sell Transaction?')) {
                        ;
                    } else {
                        window.location.href = "#page_index";
                        return;
                    }
                    var urlSt = iisurl + "cust/" + custObj.username + "/acc/" + accId + "/st/" + symbol + "/tr/" + trname + "/tran/" + sig + "/order";
                    console.log(urlSt);
                    $.ajax({
                        url: urlSt,
                        crossDomain: true,
                        cache: false,
                        beforeSend: function () {
                            $("#loader").show();
                        },
                        success: function (result) {
                            console.log(result);
                            window.location.href = "accountsttr_1.html";
                        }
                    });
                }
            }
            if (type === "graph") {
                if (trname != null) {
                    var symbol = stockObj.symbol;
                    $("#graphheader").html("Display graph - " + stockObj.symbol);
                    symbol = symbol.replace(".", "_");
                    var resultURL = iisurl + "cust/" + custObj.username + "/acc/" + accId + "/st/" + symbol + "/tr/" + trname + "/tran/history/chart";
//                resultURL = "https://iiswebsrv.herokuapp.com/cust/guest/acc/3/st/hou_to/tr/tr_macd/tran/history/chart";
                    $("#spaceimage").attr("src", resultURL);

                    window.location.href = "#page_graph";
                }
            }
            if (type === "table") {
                if (trname != null) {
                    var symbol = stockObj.symbol;
                    $("#tablehheader").html("Trading Performance - " + stockObj.symbol);

                    symbol = symbol.replace(".", "_");
                    var urlSt = iisurl + "cust/" + custObj.username + "/acc/" + accId + "/st/" + symbol + "/tr/" + trname + "/perf";
                    console.log(urlSt);
                    $.ajax({
                        url: urlSt,
                        crossDomain: true,
                        cache: false,
                        beforeSend: function () {
                            $("#loader").show();
                        },
                        success: function (resultPerfList) {
                            console.log(resultPerfList);
                            if (resultPerfList != null) {
                                var PerfObj = resultPerfList[0];
                                var trsignal = PerfObj.performData.trsignal;
                                var perfStart = PerfObj.performData.fromdate;
                                var perfEnd = PerfObj.updatedatedisplay;

                                var balance = PerfObj.balance;
                                var shareAmount = 0;
                                if (trsignal == 1) {
                                    shareAmount = PerfObj.performData.share * PerfObj.performData.close;
                                    balance += shareAmount;
                                }
                                if (trsignal == 2) {
                                    shareAmount = PerfObj.performData.share * PerfObj.performData.close;
                                    balance += shareAmount;
                                }
                                // so that to show the balacne positive
                                balance += PerfObj.investment;
                                
                                var balanceSt = Number(balance).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                                var netprofitSt = Number(PerfObj.grossprofit).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                                var investmentSt = Number(shareAmount).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                                
                                var percent = 100 * (PerfObj.grossprofit / TRADING_AMOUNT);
                                var percentSt = Number(percent.toFixed(2)).toLocaleString('en');

                                var htmlName = "";
                                htmlName += '<div class="ui-grid-a">';
                                htmlName += '<div class="ui-block-a" ><strong>' + 'Netprofit: ' + netprofitSt + '</strong></div>';
                                htmlName += '<div class="ui-block-b" >Profit(%): ' + percentSt + '%</div>';
                                htmlName += '</div>';
                                htmlName += '<br>';
                                htmlName += '<div class="ui-grid-a">';
                                htmlName += '<div class="ui-block-a" >From: ' + perfStart + '</div>';
                                htmlName += '<div class="ui-block-b" >To: ' + perfEnd + '</div>';
                                htmlName += '</div>';
                                htmlName += '<div class="ui-grid-a">';
                                htmlName += '<br>';
                                htmlName += '<div class="ui-grid-a">';
                                htmlName += '<div class="ui-block-a" >' + 'Balance: ' + balanceSt + '</div>';
                                htmlName += '<div class="ui-block-b" >' + 'Invest: ' + investmentSt + '</div>';
                                htmlName += '</div>';

                                htmlName += '<div class="ui-grid-a">';
                                htmlName += '<div class="ui-block-a" >' + 'rating: ' + PerfObj.rating.toFixed(2) + '</div>';
                                htmlName += '<div class="ui-block-b" >' + 'numtrade: ' + PerfObj.numtrade + '</div>';
                                htmlName += '</div>';
                                htmlName += '<br>';

                                htmlName += '<div class="ui-grid-a">';
                                htmlName += '<div class="ui-block-a" >' + 'numwin: ' + PerfObj.performData.numwin + '</div>';
                                htmlName += '<div class="ui-block-b" >' + 'numloss: ' + PerfObj.performData.numloss + '</div>';
                                htmlName += '</div>';

                                htmlName += '<div class="ui-grid-a">';
                                htmlName += '<div class="ui-block-a" >' + 'maxwin: ' + PerfObj.performData.maxwin.toFixed(2) + '</div>';
                                htmlName += '<div class="ui-block-b" >' + 'maxloss: ' + PerfObj.performData.maxloss.toFixed(2) + '</div>';
                                htmlName += '</div>';

                                htmlName += '<div class="ui-grid-a">';
                                htmlName += '<div class="ui-block-a" >' + 'holdtime: ' + PerfObj.performData.holdtime + '</div>';
                                htmlName += '<div class="ui-block-b" >' + 'maxholdtime: ' + PerfObj.performData.maxholdtime + '</div>';
                                htmlName += '</div>';

                                $("#myidperf").html('<li">' + htmlName + '</li>');
                                window.location.href = "#page_table";
                            }
                        }
                    });
                    window.location.href = "#page_table";
                }
            }
        });

        $("ul[id*=myid] button").click(function () {
            buttonGraph = true;
            var trname = $(this).attr('value');
            if (trname != null) {
                var symbol = stockObj.symbol;
                symbol = symbol.replace(".", "_");
                var resultURL = iisurl + "cust/" + custObj.username + "/acc/" + accId + "/st/" + symbol + "/tr/" + trname + "/tran/history/chart";
//                resultURL = "https://iiswebsrv.herokuapp.com/cust/guest/acc/3/st/hou_to/tr/tr_macd/tran/history/chart";
                $("#spaceimage").attr("src", resultURL);

                window.location.href = "#page_graph";
            }
//            $.ajax({
//                url: "https://iiswebsrv.herokuapp.com/cust/guest/acc/3/st/hou_to/tr/tr_macd/tran/history/chart",
//                datatype: "binary",
//                beforeSend: function (xhr) {
//                    xhr.overrideMimeType("text/plain; charset=x-user-defined");
//                },
//                success: function (image) {
//
//                    var imgBase64 = $.base64.encode(image);
//                    console.log(imgBase64);
//                    window.location.href = "#page_graph";
//                },
//                error: function (xhr, text_status) {
//                    console.log("An error again " + text_status);
//                }
//            });


        });


        $("ul[id*=myid] li").click(function () {
//            alert($(this).html()); // gets innerHTML of clicked li
//            alert($(this).text()); // gets text contents of clicked li
            var nameId = $(this).attr('id');
            console.log(nameId);
            if (nameId == 0) {
//                alert(nameId);
                return;
            }
            if (buttonGraph == true) {
                buttonGraph = false;
                return;
            }

            var trObj = null;
            for (i = 0; i < trObjList.length; i++) {
                var trObjTmp = trObjList[i];
                if (trObjTmp.id == nameId) {
                    trObj = trObjTmp;
                    break;
                }
            }
            if (trObj == null) {
                return;
            }
            var trName = trObj.trname;
            var iisWebObj = {'custObjStr': custObjStr, 'accObjListStr': accObjListStr,
                'accId': accId, 'stockObjListStr': stockObjListStr, 'sockId': sockId,
                'trObjListStr': trObjListStr, 'trName': trName};

            window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
            window.location.href = "accounttran_1.html";
        });

        $("#configbtn").click(function () {
            if (custObj.username.toUpperCase() === "GUEST") {
                alert("Not supproted feature for GUEST accont");
                window.location.href = "accountsttr.html";
                return;
            }
            var trNum = trObjacc.linktradingruleid;
            var trName = "TR_NN2";
            if (trNum == 0) {
                trName = "TR_ACC";
            } else if (trNum == 2) {
                trName = "TR_MACD";
            } else if (trNum == 4) {
                trName = "TR_NN1";
            } else if (trNum == 5) {
                trName = "TR_NN2";
            }
            $('#myidtrmodel').val(trName).attr("selected", "selected");

            window.location.href = "#page_conf";
        });

//    public static final String TR_ACC = "TR_ACC";  // transaction account
//    public static final int INT_TR_ACC = 0;
//    public static final String TR_MV = "TR_MV";  // simulation 
//    public static final int INT_TR_MV = 1;
//    public static final String TR_MACD = "TR_MACD";
//    public static final int INT_TR_MACD = 2;
//    public static final String TR_RSI = "TR_RSI";
//    public static final int INT_TR_RSI = 3;
//    public static final String TR_NN1 = "TR_NN1"; //NN for MACD fast
//    public static final int INT_TR_NN1 = 4;
//    public static final String TR_NN2 = "TR_NN2"; //NN for MACD 12 26
//    public static final int INT_TR_NN2 = 5;
//    public static final String TR_NN3 = "TR_NN3"; //NN for MV
//    public static final int INT_TR_NN3 = 6;    

        $("#savesubmit").click(function () {

            var tr = $('#myidtrmodel').val();
            console.log(tr);
//"/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/linktr/{linkopt or trname}"
            var answer = confirm('Do you want to save changes?');
            if (answer) {
                ;
            } else {
                window.location.href = "#page_index";
                return;
            }
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/acc/" + accId + "/st/" + sockId + "/tr/TR_ACC/linktr/" + tr,
                crossDomain: true,
                cache: false,
                success: handleResult
            }); // use promises

            // add cordova progress indicator https://www.npmjs.com/package/cordova-plugin-progress-indicator

            function handleResult(result) {
                //MAX_ALLOW_STOCK_ERROR = 100 ; NEW = 1; EXISTED = 2
                console.log(result);
                if (result == 1) {
                    window.location.href = "accountsttr_1.html";
                }
                window.location.href = "accountsttr_1.html";
            }
        });
// example        
//alert("AJAX request successfully completed");
//var jsonObj = JSON.parse(jsonStr);
//var jsonPretty = JSON.stringify(jsonObj, null, '\t');

    }
};
app.initialize();

