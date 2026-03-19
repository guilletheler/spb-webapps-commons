$ORIGINAL_JAVA_HOME = [System.Environment]::GetEnvironmentVariable("JAVA_HOME")

[System.Environment]::SetEnvironmentVariable("JAVA_HOME", [System.Environment]::GetEnvironmentVariable("JAVA21_HOME"))

Set-Location webapps-commons
mvn clean install

Set-Location ..

[System.Environment]::SetEnvironmentVariable("JAVA_HOME", $ORIGINAL_JAVA_HOME)