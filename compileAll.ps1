
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", [System.Environment]::GetEnvironmentVariable("JAVA17_HOME"))

Set-Location webapps-commons
mvn clean install
Set-Location ..

Set-Location pf-commons
mvn clean install
Set-Location ..

[System.Environment]::SetEnvironmentVariable("JAVA_HOME", [System.Environment]::GetEnvironmentVariable("JAVA21_HOME"))