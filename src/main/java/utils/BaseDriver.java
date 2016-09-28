package utils;

import java.awt.Desktop;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.xml.bind.annotation.XmlTransient;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.testng.ITestContext;

import com.perfectomobile.selenium.util.EclipseConnector;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;

public  class BaseDriver {

	private static final String HTTPS = "https://";
	private static final String MEDIA_REPOSITORY = "/services/repositories/media/";
	private static final String UPLOAD_OPERATION = "operation=upload&overwrite=true";
	private static final String UTF_8 = "UTF-8";
	public AppiumDriver<?> driver = null;
	public Map<String, Object> params = new HashMap<>();
	public Map<String, String> testParams;
	public String appName;
	public static String driverType;
	@XmlTransient public Properties property;

	/**
	 * Download the report. 
	 * type - pdf, html, csv, xml
	 * Example: downloadReport(driver, "pdf", "C:\\test\\report");
	 * 
	 */
	public static void downloadReport(RemoteWebDriver driver, String type, String fileName) throws IOException {
		try { 
			String command = "mobile:report:download"; 
			Map<String, Object> params = new HashMap<>(); 
			params.put("type", type); 
			String report = (String)driver.executeScript(command, params); 
			File reportFile = new File(fileName + "." + type); 
			BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(reportFile)); 
			byte[] reportBytes = OutputType.BYTES.convertFromBase64Png(report); 
			output.write(reportBytes);
			output.close();
		} catch (Exception ex) { 
			System.out.println("Got exception " + ex); }
	}

	/**
	 * Download all the report attachments with a certain type.
	 * type - video, image, vital, network
	 * Examples:
	 * downloadAttachment(driver, "video", "C:\\test\\report\\video", "flv");
	 * downloadAttachment(driver, "image", "C:\\test\\report\\images", "jpg");
	 */
	public static void downloadAttachment(RemoteWebDriver driver, String type, String fileName, String suffix) throws IOException {
		try {
			String command = "mobile:report:attachment";
			boolean done = false;
			int index = 0;

			while (!done) {
				Map<String, Object> params = new HashMap<>();	

				params.put("type", type);
				params.put("index", Integer.toString(index));

				String attachment = (String)driver.executeScript(command, params);

				if (attachment == null) { 
					done = true; 
				}
				else { 
					File file = new File(fileName + index + "." + suffix); 
					BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file)); 
					byte[] bytes = OutputType.BYTES.convertFromBase64Png(attachment);	
					output.write(bytes); 
					output.close(); 
					index++; }
			}
		} catch (Exception ex) { 
			System.out.println("Got exception " + ex); 
		}
	}


	/**
	 * Uploads a file to the media repository.
	 * Example:
	 * uploadMedia("demo.perfectomobile.com", "john@perfectomobile.com", "123456", "C:\\test\\ApiDemos.apk", "PRIVATE:apps/ApiDemos.apk");
	 */
	public static void uploadMedia(String host, String user, String password, String path, String repositoryKey) throws IOException {
		File file = new File(path);
		byte[] content = readFile(file);
		uploadMedia(host, user, password, content, repositoryKey);
	}

	/**
	 * Uploads a file to the media repository.
	 * Example:
	 * URL url = new URL("http://file.appsapk.com/wp-content/uploads/downloads/Sudoku%20Free.apk");
	 * uploadMedia("demo.perfectomobile.com", "john@perfectomobile.com", "123456", url, "PRIVATE:apps/ApiDemos.apk");
	 */
	public static void uploadMedia(String host, String user, String password, URL mediaURL, String repositoryKey) throws IOException {
		byte[] content = readURL(mediaURL);
		uploadMedia(host, user, password, content, repositoryKey);
	}

	/**
	 * Uploads content to the media repository.
	 * Example:
	 * uploadMedia("demo.perfectomobile.com", "john@perfectomobile.com", "123456", content, "PRIVATE:apps/ApiDemos.apk");
	 */
	public static void uploadMedia(String host, String user, String password, byte[] content, String repositoryKey) throws UnsupportedEncodingException, MalformedURLException, IOException {
		if (content != null) {
			String encodedUser = URLEncoder.encode(user, "UTF-8");
			String encodedPassword = URLEncoder.encode(password, "UTF-8");
			String urlStr = HTTPS + host + MEDIA_REPOSITORY + repositoryKey + "?" + UPLOAD_OPERATION + "&user=" + encodedUser + "&password=" + encodedPassword;
			URL url = new URL(urlStr);

			sendRequest(content, url);
		}
	}

	/**
	 * Sets the execution id capability
	 */
	public static void setExecutionIdCapability(DesiredCapabilities capabilities, String host) throws IOException {
		EclipseConnector connector = new EclipseConnector();
		String eclipseHost = connector.getHost();
		if ((eclipseHost == null) || (eclipseHost.contains(host))) {
			String executionId = connector.getExecutionId();
			capabilities.setCapability(EclipseConnector.ECLIPSE_EXECUTION_ID, executionId);
		}
	}

	private static void sendRequest(byte[] content, URL url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/octet-stream");
		connection.connect();
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		outStream.write(content);
		outStream.writeTo(connection.getOutputStream());
		outStream.close();
		int code = connection.getResponseCode();
		if (code > HttpURLConnection.HTTP_OK) {
			handleError(connection);
		}
	}

	private static void handleError(HttpURLConnection connection) throws IOException {
		String msg = "Failed to upload media.";
		InputStream errorStream = connection.getErrorStream();
		if (errorStream != null) {
			InputStreamReader inputStreamReader = new InputStreamReader(errorStream, UTF_8);
			BufferedReader bufferReader = new BufferedReader(inputStreamReader);
			try {
				StringBuilder builder = new StringBuilder();
				String outputString;
				while ((outputString = bufferReader.readLine()) != null) {
					if (builder.length() != 0) {
						builder.append("\n");
					}
					builder.append(outputString);
				}
				String response = builder.toString();
				msg += "Response: " + response;
			}
			finally {
				bufferReader.close();
			}
		}
		throw new RuntimeException(msg);
	}

	private static byte[] readFile(File path) throws FileNotFoundException, IOException {
		int length = (int)path.length();
		byte[] content = new byte[length];
		InputStream inStream = new FileInputStream(path);
		try {
			inStream.read(content);
		}
		finally {
			inStream.close();
		}
		return content;
	}

	private static byte[] readURL(URL url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setDoOutput(true);
		int code = connection.getResponseCode();
		if (code > HttpURLConnection.HTTP_OK) {
			handleError(connection);
		}
		InputStream stream = connection.getInputStream();

		if (stream == null) {
			throw new RuntimeException("Failed to get content from url " + url + " - no response stream");
		}
		byte[] content = read(stream);
		return content;
	}

	private static byte[] read(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[1024];
			int nBytes = 0;
			while ((nBytes = input.read(buffer)) > 0) {
				output.write(buffer, 0, nBytes);
			}
			byte[] result = output.toByteArray();
			return result;
		} finally {
			try{
				input.close();
			} catch (IOException e){

			}
		}
	}

	/**
	 * @param locator
	 * @param timeout
	 * @description  Waits for objects to load before proceding !!! 
	 */
	public static WebElement fluentWait(final By locator, AppiumDriver<WebElement> driver, long timeout) {	 

		Wait<WebDriver> await = new FluentWait<WebDriver> (driver)
				.withTimeout(timeout, TimeUnit.SECONDS)
				.pollingEvery(500, TimeUnit.MILLISECONDS)
				.ignoring(NoSuchElementException.class);
		try {	
			await.until(ExpectedConditions.visibilityOf(driver.findElement(locator)));
			return driver.findElement(locator);
		}catch(Exception e){
			return null;
		}
	}

	/**
	 * @param context 
	 * @description Creates the driver
	 */
	public AppiumDriver<?> driverObj(ITestContext context)
			throws Exception {
		testParams = context.getCurrentXmlTest().getAllParameters();		
		DesiredCapabilities capabilities = new DesiredCapabilities();
		property = (Properties) System.getProperties().clone();
		appName = testParams.get("perfect.app");
		capabilities.setCapability("user", testParams.get("perfecto.username"));
		capabilities.setCapability("password", property.getProperty("perfecto.password"));
		if(!testParams.get("deviceName").isEmpty()){
			capabilities.setCapability("deviceName", testParams.get("deviceName"));
		}else{
			capabilities.setCapability("platformName", testParams.get("platformName"));
			capabilities.setCapability("platformVersion", testParams.get("platformVersion"));
			capabilities.setCapability("manufacturer", testParams.get("manufacturer"));
			capabilities.setCapability("model", testParams.get("model"));
		}
		capabilities.setCapability("bundleId", testParams.get("package"));
		capabilities.setCapability(WindTunnelUtils.WIND_TUNNEL_PERSONA_CAPABILITY, WindTunnelUtils.GEORGIA);
		capabilities.setCapability("automationName", "Appium");
		if (testParams.get("RunMode").equals("Debug")) {
			setExecutionIdCapability(capabilities, testParams.get("perfecto.url"));
		} 		
		String proxyHost=testParams.get("proxy.url");	
		String proxyPort=testParams.get("proxy.port");
		final String proxyUser=testParams.get("proxy.user");
		final String proxyPassword=testParams.get("proxy.pass");
		String perfectoHost=testParams.get("perfecto.url");

		if(!proxyHost.isEmpty()) {
			System.setProperty("http.proxyHost", proxyHost);
			System.setProperty("http.proxyPort", proxyPort);
			System.setProperty("https.proxyHost", proxyHost);
			System.setProperty("https.proxyPort", proxyPort);
			if(!proxyUser.isEmpty()) {
				System.out.println("\n\n\nProxy user is " + proxyUser);
				perfectoHost = proxyUser + ":" + URLEncoder.encode(proxyPassword, "UTF-8") + "@" + perfectoHost;
				Authenticator authenticator = new Authenticator() {
					public PasswordAuthentication getPasswordAuthentication() {
						System.out.println("\n\n\nauth function called");
						return (new PasswordAuthentication(proxyUser, proxyPassword.toCharArray()));
					}
				};
				Authenticator.setDefault(authenticator);
			}
		}
		boolean waitForDevice = true;
		int retries = 3;
		int retryIntervalSec = 1;
		driverType = testParams.get("driverType");
		do {
			try {		
				if (driverType.equals("IOS")) {
					driver = new IOSDriver<>(new URL("https://" + perfectoHost + "/nexperience/perfectomobile/wd/hub"), capabilities); 
				}else{
					driver = new AndroidDriver<>(new URL("https://" + perfectoHost + "/nexperience/perfectomobile/wd/hub"), capabilities);
				}					
				if (!(driver == null)) {
					waitForDevice = false;
				}
			} catch (Exception e) {
				retries--;
				System.out.println("\n\nDevice in use....reconnecting again....: " + capabilities.toString() + "\n Retries Left: " + retries);
				sleep(retryIntervalSec * 1000);
				if(retries < 0) {
					waitForDevice = false;
				}
			}
		} while(waitForDevice); 
		driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
		driver.manage().timeouts().pageLoadTimeout(20, TimeUnit.SECONDS);
		driver.manage().timeouts().setScriptTimeout(20, TimeUnit.SECONDS);
		driver.context("NATIVE_APP");
		return driver;	
	}


	/**
	 * @param millis
	 */
	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}	

	public void tearDown() throws IOException {
		String reportURL = (String)(driver.getCapabilities().getCapability(WindTunnelUtils.WIND_TUNNEL_REPORT_URL_CAPABILITY));
		System.out.println(reportURL);

		if(!(driver == null)){
			driver.close();			
			BaseDriver.downloadReport(driver, "pdf", "C:\\temp\\report");
			driver.quit();
		}
		Desktop desktop = Desktop.getDesktop();
		desktop.browse(URI.create(reportURL));
	}

	public void init(int implicitWaitTime){
		driver.manage().timeouts().pageLoadTimeout(20, TimeUnit.SECONDS);
		implicitWait(implicitWaitTime);		
	}

	public void implicitWait(int time) {
		driver.manage().timeouts().implicitlyWait(time, TimeUnit.SECONDS);
	}


	public void launchApp() throws InterruptedException{
		Map<String, Object> app = new HashMap<>();	
		app.put("name", appName);
		((JavascriptExecutor) driver).executeScript("mobile:application:open", app);		
	}

	public void closeApp(){
		Map<String, Object> closeApp = new HashMap<>();	
		closeApp.put("name", appName);		
		((JavascriptExecutor) driver).executeScript("mobile:application:close", closeApp);
	}
}