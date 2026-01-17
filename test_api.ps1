# Test Script for QR Generator API

# 1. Create a dummy test file
$testFile = "test_image.txt"
"This is a dummy image content for testing" | Out-File -Encoding UTF8 $testFile

# 2. Define the API URL
$url = "http://localhost:8080/api/qr/generate"

# 3. Check if the server is running (simplistic check)
Write-Host "Testing API at $url..."
Write-Host "Make sure the Spring Boot app is running (mvn spring-boot:run) before running this script."

# 4. specific curl command for Windows
# Windows curl aliases to Invoke-WebRequest in PowerShell sometimes, so we use explict curl.exe or Invoke-RestMethod.
# Using standard curl if available, otherwise Invoke-RestMethod

try {
    # Attempt using Invoke-RestMethod for native PowerShell support
    $response = Invoke-RestMethod -Uri $url -Method Post -InFile $testFile -ContentType "multipart/form-data" 
    # Note: multippart/form-data with Invoke-RestMethod is tricky in older PS. 
    # Let's fallback to curl.exe which is standard on modern Windows.
    
    Write-Host "Please ensure 'curl' is installed or use the following command manually:"
    Write-Host "curl -X POST -F 'image=@$testFile' $url"
    
    # Actually executing curl
    & curl -X POST -F "image=@$testFile" $url
}
catch {
    Write-Error "Error during request: $_"
}
