
# Gatekeeper Compose Email backend

This service is a back end for the Gatekeeper Compose Email front end. It takes an email composed in that service, which may include markup in its body, and calls the 
 appropriate renderer to convert it to HTML then deals with sending it to the recipient(s) using the HMRC email service.
It stores every email sent through it.

It uses the [Digital Contact teams's email service](https://github.com/hmrc/email) to perform the actual sending of the email. 
This is done gradually by means of a scheduled job which runs once per second (configurable). The scheduled job, when it runs, attempts to send
one email to the email service. If the response from the email service is not 202 then the email a further attempt will be 
made the next time the scheduled job runs. If the email service continues to respond with a non-202 status a further 2 times 
then the email will be marked as failed and not retried.  

## Testing locally
It can most easily be tested by going through Gatekeeper to send an email, either to a selected group of recipients or to all
Developer Hub users.

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

