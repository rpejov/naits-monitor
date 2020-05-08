# naits-monitor
tool for monitoring servers. 

The naits.properties file must be in the working folder as the JAR. 
You can add up to 10 URLs with the naming pattern url0, url1, url2 ..etc.
You should configure your e-mail parameters to be able to send e-mails.
For gmail you must allow "Less secure apps" in the settings of your account.

Configure URLs in properties file:
--
*url0=http://192.168.100.160:8030/
*url1=
*url2=
.....

Configure, mail from, user, pass and mail server
--
*mail.from = admin@gmail.com
*mail.username =admin@gmail.com
*mail.password =admin_pass
*mail.host = smtp.gmail.com
*mail.smtp.auth=true
*mail.smtp.starttls.enable=true
*mail.smtp.port=587
*mail.format=text/html; charset=UTF-8;

Set the regex to match from the HTTP response of the urls
-- 
*response.regex =\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b

If regex is not matched, send e-mail to, with subject and body.
--
*mail.to=monitor@gmail.com	
*mail.subject=Server down
8mail.body=Server down: {url}. Request to server didn't finish with valid response. Result: {response}



