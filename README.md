
# Gatekeeper Compose Email backend

This service is a back end for the Gatekeeper Compose Email front end. It takes an email composed in that service, which may include markup in its body, and calls the 
 appropriate renderer to convert it to HTML then deals with sending it to the recipient(s) using the HMRC email service.
It stores every email sent through it.

## Testing locally
It can be tested with a curl command like:
```
curl localhost:9620/gatekeeper-email -d '{"to":["sdgdf@sdfds"], "templateId":"gatekeeper", "emailData":{"emailRecipient":"to","emailSubject":"subject","emailBody":"body"},"force":false,"auditData":{"foo":"bar"},"eventUrl":"sdfas"}' -H "Content-Type:application/json"
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

### Run object store stub 
sm --start OBJECT_STORE_STUB


## check out internal-auth git repo 
## run command:
sbt "run 8470" -Dapplication.router=testOnlyDoNotUseInAppConf.Routes


##POSTMAN REQUEST

POST to  http://localhost:8470/test-only/token
with body:
{
"token": "1113",
"principal": "object-store",
"permissions": [{
"resourceType": "object-store",
"resourceLocation": "gatekeeper-email",
"actions": ["*"]
}]
}

Response:
{
"token": "1113",
"expiresAt": "2022-03-14T12:20:19.832Z"
}

## the token 1113 should be in application.conf ( which is set currently)

