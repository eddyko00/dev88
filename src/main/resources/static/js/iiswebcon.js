
var app = {

// Application Constructor
    initialize: function () {

        $(document).ready(function () {


//            var iisWebSession = "iisWebSession";
//            var iisWebObjStr = window.localStorage.getItem(iisWebSession);
//            var iisWebObj = JSON.parse(iisWebObjStr);
//            var custObjStr = iisWebObj1.custObjStr;
//            var custObj = JSON.parse(custObjStr);
//            
            document.getElementById("txt-first-name").setAttribute('value', custObj.firstname);
            document.getElementById("txt-last-name").setAttribute('value', custObj.lastname);

            document.getElementById("txt-email-address-signup").setAttribute('value', custObj.email);
            document.getElementById("txt-password-signup").setAttribute('value', custObj.password);

//            document.getElementById("txt-first-name").setAttribute('value', 'firstname');
//            document.getElementById("txt-last-name").setAttribute('value', 'lastname');
//            
//            document.getElementById("txt-email-address-signup").setAttribute('value', 'email');
//            document.getElementById("txt-password-signup").setAttribute('value', 'password');

        });


//        var iisurl = "https://iiswebsrv.herokuapp.com/";
        var iisMsgSession = "iisMsgSession";
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
        var accId = iisWebObj.accId;
        console.log(accId);

        var iisMsgSession = "iisMsgSession";
        var msgObjStr = window.localStorage.getItem(iisMsgSession);
//        msgObjStr ="This feature does not allow for GUEST account";
        if (msgObjStr !== "") {
            functionAlertConfirm(msgObjStr, function ok() {
            });
        }

        $("#btn-submit").click(function () {
            var txtfirstname = document.getElementById("txt-first-name").value;
            var txtlastname = document.getElementById("txt-last-name").value;
            var txtemailaddress = document.getElementById("txt-email-address-signup").value;
            var txtpassword = document.getElementById("txt-password-signup").value;


//          ""/cust/{username}/acc/{accountid}/custupdate?email=&pass=&firstName=&lastName=&plan=""
//          SUCC = 1;  EXISTED = 2; FAIL =0;
            $.ajax({
                url: iisurl + "/cust/" + custObj.username + "/acc/" + accId + "/custupdate?email=" + txtemailaddress + "&pass=" + txtpassword + "&firstName=" + txtfirstname + "&lastName=" + txtlastname,
                crossDomain: true,
                cache: false,
                success: handleResult
            }); // use promises

            // add cordova progress indicator https://www.npmjs.com/package/cordova-plugin-progress-indicator

            function handleResult(result) {
//          SUCC = 1;  EXISTED = 2; FAIL =0;
                var webMsg = result.webMsg;
                console.log(webMsg);
                var resultID = webMsg.resultID;

                msgObjStr = '';
                // 0 - general fail, 1 - successful, 2 - email fail 3 - password
                if (resultID === 0) {
                    msgObjStr = "Update failed. Please try again.";
                }
                if (resultID === 2) {
                    msgObjStr = "Update failed. Please correct the email address.";
                }
                if (resultID === 3) {
                    msgObjStr = "Update failed. Please use another password.";
                }                
                var iisWebObj = {'custObjStr': custObjStr, 'iisurlStr': iisurlStr, 'accObjListStr': accObjListStr, 'accId': accId};
                window.localStorage.setItem(iisWebSession, JSON.stringify(iisWebObj));
                 window.localStorage.setItem(iisMsgSession, msgObjStr);
                window.location.href = "conf_1.html";

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





