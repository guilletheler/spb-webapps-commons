
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", [System.Environment]::GetEnvironmentVariable("JAVA17_HOME"))

cd webapps-commons
mvn clean install

cd ..

[System.Environment]::SetEnvironmentVariable("JAVA_HOME", [System.Environment]::GetEnvironmentVariable("JAVA21_HOME"))