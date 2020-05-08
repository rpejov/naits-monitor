package naits.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Map.Entry;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class NaitsMonitor {

	static Properties sysProps = loadProperties();

	private static final String USER_AGENT = "Mozilla/5.0";

	/**
	 * Method to do the initial reading of the naits.properties file
	 * 
	 * @return Properties object
	 */
	static Properties loadProperties() {

		Properties mainProperties = null;
		InputStream props = null;
		boolean hasErrors = true;
		try {
			URL propsUrl = NaitsMonitor.class.getClassLoader().getResource("naits.properties");
			if (propsUrl != null) {

				System.out.println("Loading configuration from:" + propsUrl.getFile());
				props = NaitsMonitor.class.getClassLoader().getResourceAsStream("naits.properties");
			}
			if (props == null) {
				String path = "./naits.properties";
				File pFile = new File(path);
				if (pFile != null) {
					System.out.println("Loading configuration from:" + pFile.getCanonicalPath());
					props = NaitsMonitor.class.getClassLoader().getResourceAsStream("naits.properties");
				}
				props = new FileInputStream(pFile);
			}

			// load all the properties from this file
			Properties temp = new Properties();
			temp.load(props);
			// all well
			mainProperties = temp;
			hasErrors = false;
		} catch (Exception e) {
			System.out.println("naits.properties config file can not be found. Naits not initialised.");
			e.printStackTrace();
		} finally {
			try {
				if (props != null)
					props.close();
			} catch (IOException e) {
				System.out.println("naits.properties config file not properly read. Naits initialisation error.");
				e.printStackTrace();
				hasErrors = true;
			}
		}
		if (hasErrors)
			mainProperties = null;
		return mainProperties;
	}

	static void sendMail(String recipientAddress, String mailSubject, String mailBody,
			HashMap<String, String> extParams) {

		try {
			for (Entry<String, String> ent : extParams.entrySet()) {
				mailBody = mailBody.replace(ent.getKey(), ent.getValue());
			}
			// Sender's email ID needs to be mentioned
			String from = sysProps.getProperty("mail.from").trim();
			final String username = sysProps.getProperty("mail.username").trim();
			final String password = sysProps.getProperty("mail.password").trim();
			// Assuming you are sending email through relay.jangosmtp.net
			String host = sysProps.getProperty("mail.host").trim();
			Properties props = new Properties();
			props.put("mail.smtp.auth", sysProps.getProperty("mail.smtp.auth").trim());
			props.put("mail.smtp.starttls.enable", sysProps.getProperty("mail.smtp.starttls.enable").trim());
			props.put("mail.smtp.host", host);
			props.put("mail.smtp.port", sysProps.getProperty("mail.smtp.port").trim());
			props.put("mail.smtp.ssl.trust", host);

			// Get the Session object.
			Session session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			});

			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(session);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress(from));

			// Set To: header field of the header.
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientAddress));

			String mailFormat = sysProps.getProperty("mail.format") != null ? sysProps.getProperty("mail.format").trim()
					: "text/html; charset=UTF-8";
			message.setHeader("Content-Type", "text/html; charset=UTF-8");

			// Set Subject: header field
			message.setSubject(mailSubject, "UTF-8");

			// Now set the actual message
			message.setContent(mailBody, mailFormat);

			// Send message
			Transport.send(message);

			System.out.println("Sent message successfully....");

		} catch (Exception e) {
			System.out.println("Sending mail failed");
			e.printStackTrace();
		}
	}

	private static String sendGET(String getUrl) throws IOException {
		URL obj = new URL(getUrl);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		StringBuffer response = new StringBuffer();
		con.setRequestProperty("User-Agent", USER_AGENT);
		int responseCode = con.getResponseCode();
		System.out.println("GET Response Code :: " + responseCode);
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

		} else
			response.append(
					"Error executing HTTP request. The response code was not 200!. Response Code :: " + responseCode);
		return response.toString();

	}

	public static void main(String[] args) throws Exception {
		if (sysProps == null) {
			System.out.println("naits.properties config file can not be found. Naits not initialised.");
			System.exit(1);
		}
		// load all urls from config
		ArrayList<String> urls = new ArrayList<String>();
		for (int i = 0; i <= 10; i++) {
			String url = sysProps.getProperty("url" + Integer.toString(i));
			if (url != null && !url.isEmpty())
				urls.add(url.trim());
		}

		for (String url : urls) {
			String result = null;
			boolean error = false;

			try {
				result = sendGET(url);
			} catch (Exception e) {
				result = e.getMessage();
				error = true;
			}
			String regex = sysProps.getProperty("response.regex");
			boolean isResponseGood = result.matches(regex);
			if (!isResponseGood || error == true) {
				System.out.println("Server:" + url + " didn't respond properly. Sending alert e-mail to:"
						+ sysProps.getProperty("mail.to"));
				HashMap extParams = new HashMap<String, String>();
				extParams.put("{url}", url);
				extParams.put("{response}", result);
				sendMail(sysProps.getProperty("mail.to"), sysProps.getProperty("mail.subject"),
						sysProps.getProperty("mail.body"), extParams);
			}
		}

	}
}
