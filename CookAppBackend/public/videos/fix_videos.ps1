$videos = Get-ChildItem -Filter *.mp4
mkdir fixed -ErrorAction SilentlyContinue
foreach ($v in $videos) {
    Write-Host "Fixing $($v.Name) (Re-encoding audio track & Fixing Indexes)..."
    ffmpeg -y -v warning -i $v.FullName -c:v copy -c:a aac -b:a 128k -movflags +faststart "fixed\$($v.Name)"
}
Write-Host "Done fixing files! Moving them back..."
foreach ($v in $videos) {
    if (Test-Path "fixed\$($v.Name)") {
        Move-Item -Path "fixed\$($v.Name)" -Destination $v.FullName -Force
    }
}
Write-Host "All videos fixed and replaced!"
