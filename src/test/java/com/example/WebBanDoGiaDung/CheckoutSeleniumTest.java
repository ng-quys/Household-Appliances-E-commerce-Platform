package com.example.WebBanDoGiaDung;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckoutSeleniumTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        // Lấy baseUrl từ System Property hoặc mặc định là http://localhost:8080
        baseUrl = System.getProperty("baseUrl", "http://localhost:8080");

        ChromeOptions options = new ChromeOptions();
        // Cấu hình chạy headless (không giao diện) để test chạy mượt mà trên server hoặc CI.
        // Bạn có thể comment dòng dưới nếu muốn xem trực quan trình duyệt chạy trên máy.
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.manage().window().maximize();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void testPurchaseFlow() throws InterruptedException {
        long timestamp = System.currentTimeMillis();
        String testEmail = "testuser_" + timestamp + "@example.com";
        String testPassword = "Password123";

        // 1. Đăng ký tài khoản mới
        driver.get(baseUrl + "/register");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("name"))).sendKeys("Test User " + timestamp);
        driver.findElement(By.id("email")).sendKeys(testEmail);
        driver.findElement(By.id("phone")).sendKeys("0987654321");
        driver.findElement(By.id("password")).sendKeys(testPassword);
        driver.findElement(By.id("confirmPassword")).sendKeys(testPassword);

        // Click submit button
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Đợi chuyển hướng sang trang login
        wait.until(ExpectedConditions.urlContains("/login"));

        // 2. Đăng nhập với tài khoản vừa tạo
        driver.findElement(By.id("email")).sendKeys(testEmail);
        driver.findElement(By.id("password")).sendKeys(testPassword);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Chờ đăng nhập thành công và chuyển hướng
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlToBe(baseUrl + "/"),
                ExpectedConditions.urlContains("/home"),
                ExpectedConditions.urlContains("/profile")
        ));

        // 3. Thêm địa chỉ giao hàng mặc định (bắt buộc trước khi đặt hàng)
        driver.get(baseUrl + "/profile/addresses");

        // Click nút "Thêm thông tin địa chỉ giao hàng mới"
        WebElement addAddressBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Thêm thông tin địa chỉ')]")
        ));
        addAddressBtn.click();

        // Chờ form hiển thị và điền thông tin
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("addressName"))).sendKeys("Test User Address");
        driver.findElement(By.id("addressPhone")).sendKeys("0987654321");

        // Chọn Tỉnh/Thành phố
        WebElement provinceSelect = driver.findElement(By.id("provinceSelect"));
        Select selectProvince = new Select(provinceSelect);
        selectProvince.selectByIndex(1); // Chọn tỉnh đầu tiên có dữ liệu

        // Chờ Quận/Huyện tải xong và chọn
        Thread.sleep(1500); // Đợi gọi AJAX tải Quận/Huyện
        WebElement districtSelect = driver.findElement(By.id("districtSelect"));
        wait.until(d -> new Select(districtSelect).getOptions().size() > 1);
        new Select(districtSelect).selectByIndex(1);

        // Chờ Phường/Xã tải xong và chọn
        Thread.sleep(1500); // Đợi gọi AJAX tải Phường/Xã
        WebElement wardSelect = driver.findElement(By.id("wardSelect"));
        wait.until(d -> new Select(wardSelect).getOptions().size() > 1);
        new Select(wardSelect).selectByIndex(1);

        driver.findElement(By.id("addressContent")).sendKeys("123 Đường Test, Phường 1");

        // Check checkbox "Đặt làm địa chỉ mặc định"
        WebElement defaultAddressCheckbox = driver.findElement(By.id("addressDefault"));
        if (!defaultAddressCheckbox.isSelected()) {
            defaultAddressCheckbox.click();
        }

        // Submit form lưu địa chỉ
        driver.findElement(By.cssSelector("#addressForm button[type='submit']")).click();

        // Đợi thông báo thành công hoặc danh sách địa chỉ hiển thị
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("address-card")));

        // 4. Chọn sản phẩm và thêm vào giỏ hàng
        driver.get(baseUrl + "/products");

        // Click vào sản phẩm đầu tiên
        List<WebElement> productLinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector(".product-card .product-image-link")
        ));
        assertTrue(productLinks.size() > 0, "Không tìm thấy sản phẩm nào trên trang danh sách sản phẩm!");
        productLinks.get(0).click();

        // Chờ trang chi tiết sản phẩm load và click nút "Thêm vào giỏ"
        WebElement addToCartBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[type='submit'].btn-warning")
        ));
        addToCartBtn.click();

        // Chờ giỏ hàng cập nhật số lượng
        Thread.sleep(1500);

        // 5. Đi tới trang giỏ hàng
        driver.get(baseUrl + "/cart");

        // Đợi trang giỏ hàng hiển thị
        wait.until(ExpectedConditions.urlContains("/cart"));

        // Chọn phương thức thanh toán COD
        WebElement codRadio = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[type='radio'][value='COD']")
        ));
        if (!codRadio.isSelected()) {
            codRadio.click();
        }

        // Click đặt hàng (checkout)
        WebElement checkoutBtn = driver.findElement(By.cssSelector("button[type='submit'].btn-primary"));
        checkoutBtn.click();

        // 6. Kiểm tra xem có chuyển hướng thành công về trang profile lịch sử đơn hàng không
        wait.until(ExpectedConditions.urlContains("/profile"));
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/profile"), "Thanh toán COD thất bại! Trình duyệt không chuyển hướng về /profile.");
    }
}
