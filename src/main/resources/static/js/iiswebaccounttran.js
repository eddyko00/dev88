
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

        var trName = iisWebObj.trName;

        var tranObjListStr = iisWebObj.tranObjListStr;
        var tranObjList = JSON.parse(tranObjListStr);


        var close = stockObj.afstockInfo.fclose;
        var preClose = stockObj.prevClose;
        var percent = 100 * (close - preClose) / preClose;
        if (percent > 0) {
            percentSt = "<font style= color:green>" + percent.toFixed(2) + '%' + "</font>";
        } else {
            percentSt = "<font style= color:red>" + percent.toFixed(2) + '%' + "</font>";
        }

        var stStr = 'Trading Model Transaction Listing<br>';
        stStr += stockObj.stockname + '<br>' + stockObj.updateDateD + '<br>' +
                'Pre Cl:' + preClose + '  Close:' + close + '  Per:' + percentSt;
        $("#0").html('<h1>' + stStr + '</h1>');

        $("#accheader").html(stockObj.symbol + " " + trName);

        var htmlhead = '<div class="ui-grid-c">';
        htmlhead += '<div class="ui-block-a" "><strong>Date</strong></div>';
        htmlhead += '<div class="ui-block-b" style="text-align: center">Sig</div>';
        htmlhead += '<div class="ui-block-c" style="width:25%">Price</div>';
        htmlhead += '<div class="ui-block-d" style="width:25%">Profit</div>';

        htmlhead += '</div>';
        $("#myid").append('<li id="0" >' + htmlhead + '</li>');

        var j = tranObjList.length - 1;
        var prevTranObj = null;
        var total = 0;

        var list = [];

        var buyOnly = 0;
        if (trName === "TR_ACC") {
            buyOnly = 1;
        }

        for (i = 0; i < tranObjList.length; i++) {
            var tranObj = tranObjList[j - i];
            console.log(tranObj);
            var nameId = tranObj.id;


            var tranhtml = '';
//https://demos.jquerymobile.com/1.1.2/docs/content/content-grids.html
            var htmlName = '<div class="ui-grid-c">';
            htmlName += '<div class="ui-block-a" ><strong>' + tranObj.entrydatedisplay + '</strong></div>';
            var signal = "B";
            if (tranObj.trsignal === S_BUY) {
                signal = "B";
            } else if (tranObj.trsignal === S_SELL) {
                signal = "S";
                if (buyOnly === 1) {
                    // assume buy only and no short selling
                    prevTranObj = tranObj;
                    continue;
                }
            } else {
                signal = "E";
            }

            if (signal === "E") {
                if (prevTranObj != null) {
                    var diff = (tranObj.avgprice - prevTranObj.avgprice) * tranObj.share;
                    if (prevTranObj.trsignal === S_BUY) {
                        ;
                    }
                    if (prevTranObj.trsignal === S_SELL) {
                        diff = -diff;
                        if (buyOnly === 1) {
                            // assume buy only and no short selling
                            diff = 0;
                            prevTranObj = tranObj;
                            continue;
                        }
                    }
                    total += diff;
//                    var totalSt = total.toFixed(2);
                    var totalSt = Number(total).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                    var diffSt = Number(diff).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                    var tran = ' Tran on loss:' + diffSt;
                    if (diff > 0) {
                        tran = ' Tran on gain:' + diffSt;
                    }
                    tranhtml += 'Share=' + tranObj.share + tran; // + ' Total: ' + totalSt;
                }
            } else {
                if (i == tranObjList.length - 1) {
                    //calculate the result on the last one
                    var diff = (close - tranObj.avgprice) * tranObj.share;
                    if (tranObj.trsignal === S_BUY) {
                        ;
                    }
                    if (tranObj.trsignal === S_SELL) {
                        diff = -diff;
                        if (buyOnly === 1) {
                            // assume buy only and no short selling
                            diff = 0;
                            prevTranObj = tranObj;
                            continue;
                        }
                    }
                    total += diff;
//                    var totalSt = total.toFixed(2);
                    var totalSt = Number(total).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
                    var diffSt = Number(diff).toLocaleString('en-US', {style: 'currency', currency: 'USD'});

                    tranhtml += 'Share=' + tranObj.share + ' Tran amt change: ' + diffSt; // + ' Total: ' + totalSt;
                }
            }
            if (tranObj.trsignal === S_BUY) {
                htmlName += '<div class="ui-block-b" style="color:green;text-align: center">:' + signal + '</div>';
            } else if (tranObj.trsignal === S_SELL) {
                htmlName += '<div class="ui-block-b" style="color:red;text-align: center">:' + signal + '</div>';
            } else {
                htmlName += '<div class="ui-block-b" style="text-align: center">:' + signal + '</div>';
            }

            var avgSt = tranObj.avgprice.toFixed(2);
            htmlName += '<div class="ui-block-c" style="width:25%">' + avgSt + '</div>';
            var totalSt = Number(total.toFixed(0)).toLocaleString('en-US', {style: 'currency', currency: 'USD'});
            totalSt = totalSt.replace(".00", "");
            htmlName += '<div class="ui-block-d" style="width:25%">' + totalSt + '</div>'; //tranObj.share + '</div>';

            htmlName += '</div>';
            htmlName += tranhtml;

            prevTranObj = tranObj;
            list.push(htmlName);

        }
        for (i = 0; i < list.length; i++) {
            var htmlSt = list[list.length - i - 1];
            $("#myid").append('<li id="' + nameId + '">' + htmlSt + '</li>');
        }


// example        
//alert("AJAX request successfully completed");
//var jsonObj = JSON.parse(jsonStr);
//var jsonPretty = JSON.stringify(jsonObj, null, '\t');

    }
};
app.initialize();

