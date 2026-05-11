$ProgressPreference = 'SilentlyContinue'
Import-Module BitsTransfer
$url = 'https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip'
$output = "$env:USERPROFILE\maven.zip"
Start-BitsTransfer -Source $url -Destination $output -Description "Downloading Maven" -TransferType Download
Expand-Archive -Path $output -DestinationPath $env:USERPROFILE -Force
Remove-Item $output
Write-Host "Maven installed successfully"
