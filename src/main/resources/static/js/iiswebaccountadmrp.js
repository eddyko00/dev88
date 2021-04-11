
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

        $("#accheader").html("Accounting Report");


        $("#commid").html(" "); //clear the field
        var htmlhead = '<div class="ui-grid-b">';
        htmlhead += '<div class="ui-block-a" style="width:30%"><strong>Date</strong></div>';
        htmlhead += '<div class="ui-block-b" >Name</div>';
        htmlhead += '<div class="ui-block-c" >Amount</div>';
        htmlhead += '</div>';

        $("#myid").html('<li id="0" >' + htmlhead + '</li>');

        if (reportObjStr !== "") {
            var reportObj = JSON.parse(reportObjStr);
            var entryList = reportObj.accTotalEntryBal;

            for (i = 0; i < entryList.length; i++) {
                var entryObj = entryList[i];
                var entryId = i + 1;

                var htmlName = '<div class="ui-grid-b">';
                htmlName += '<div class="ui-block-a" style="width:30%"><strong>' + entryObj.dateSt + '</strong></div>';
                htmlName += '<div class="ui-block-b" >' + entryObj.name + '</div>';
                htmlName += '<div class="ui-block-c" >' + entryObj.amount + '</div>';
                htmlName += '</div>';
                $("#myid").append('<li id="' + entryId + '" >' + htmlName + '</li>');

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

        });




// example        
//alert("AJAX request successfully completed");
//var jsonObj = JSON.parse(jsonStr);
//var jsonPretty = JSON.stringify(jsonObj, null, '\t');

    }
};
app.initialize();





