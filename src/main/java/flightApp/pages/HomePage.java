package flightApp.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

import io.appium.java_client.AppiumDriver;
import utils.BaseDriver;
import utils.WindTunnelUtils;

public class HomePage {
	AppiumDriver<?> driver;

	public HomePage(AppiumDriver<?> driver) {
		this.driver = driver;
	}

	// Page elements
	public By lblRecentActivity = By.xpath("//*[@label=\"Recent Activity\" or @text=\"Recent Activity\"]");
	public By lblSkymiles = By.xpath("//*[@label=\"SKYMILES\"or @text=\"SkyMiles\"]");
	public By lblMyTrips= By.xpath("//*[@label=\"MY TRIPS\"or @text=\"My Trips\"]");
	public By lblProfile = By.xpath("//*[@label=\"PROFILE\"or @text=\"Profile\"]");
	private By btnNoThanks = By.xpath("//*[@label=\"No, thanks\"or @text=\"No, thanks\"]");
	public By btnMore = By.xpath("//*[@content-desc=\"More options\" or @label=\"More\"]");
	private By btnLogout = By.xpath("//*[@text=\"Log Out\" or @label=\"LOG OUT\"]");


	public void verifyItem(By by) {		
		if(driver.findElement(by).isDisplayed()){	
			WindTunnelUtils.pointOfInterest(driver,  by.toString() + " is displayed", WindTunnelUtils.SUCCESS);
		}else{
			if(clickNoThanks()){
				verifyItem(by);
			}else{
				WindTunnelUtils.pointOfInterest(driver,  by.toString() + " is not displayed", WindTunnelUtils.FAILURE);

			}
		}
	}

	protected boolean clickNoThanks() {		
		if(BaseDriver.fluentWait(btnNoThanks, (AppiumDriver<WebElement>) driver, 5).isDisplayed()){	
			WebElement thanks = driver.findElement(btnNoThanks);		
			thanks.click();	
			return true;
		}else{
			return false;
		}
	}

	public void clickMore() {		
		WebElement more = driver.findElement(btnMore);		
		more.click();	
	}

	public void clickLogout() {		
		WebElement out = driver.findElement(btnLogout);		
		out.click();	
	}
}
