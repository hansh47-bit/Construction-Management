param(
    [string]$GmailAddress,
    [string]$AccessUrl,
    [switch]$WaitForExistingBackend,
    [switch]$PasswordWindow
)

$ErrorActionPreference = 'Stop'
$backendDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$bindingNames = @('GMAIL_ADDRESS', 'GMAIL_APP_PASSWORD', 'APP_BASE_URL', 'APPROVAL_EMAIL_ENABLED', 'GRADLE_USER_HOME')
$previousBindings = @{}
foreach ($bindingName in $bindingNames) { $previousBindings[$bindingName] = [Environment]::GetEnvironmentVariable($bindingName, 'Process') }
$securePassword = $null
$readyMarker = $null
$Host.UI.RawUI.WindowTitle = '아이엔씨 Gmail 연결'

Push-Location $backendDirectory
try {
    if (-not $WaitForExistingBackend -and (Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue)) {
        throw '프로그램 서버가 이미 실행 중입니다. 기존 서버 실행 창에서 Ctrl+C로 종료한 뒤 다시 실행하세요. 이 작업은 기존 데이터나 계정을 삭제하지 않습니다.'
    }
    if ([string]::IsNullOrWhiteSpace($GmailAddress)) { $GmailAddress = Read-Host '알림을 보낼 Gmail 주소' }
    $GmailAddress = $GmailAddress.Trim()
    if ($GmailAddress -notmatch '^[^\s@]+@[^\s@]+\.[^\s@]+$') { throw '올바른 Gmail 주소를 입력하세요.' }

    if ([string]::IsNullOrWhiteSpace($AccessUrl)) {
        $officeNetwork = Get-NetIPConfiguration -ErrorAction SilentlyContinue | Where-Object { $_.IPv4DefaultGateway -and $_.IPv4Address } | Select-Object -First 1
        $suggestedUrl = if ($officeNetwork) { 'http://' + $officeNetwork.IPv4Address[0].IPAddress + ':5173' } else { '' }
        $enteredUrl = Read-Host "직원들이 사용하는 프로그램 접속 주소 [기본값: $suggestedUrl]"
        $AccessUrl = if ([string]::IsNullOrWhiteSpace($enteredUrl)) { $suggestedUrl } else { $enteredUrl.Trim() }
    }
    $parsedUrl = $null
    if (-not [Uri]::TryCreate($AccessUrl, [UriKind]::Absolute, [ref]$parsedUrl) -or $parsedUrl.Scheme -notin @('http', 'https') -or $parsedUrl.UserInfo -or $parsedUrl.Query -or $parsedUrl.Fragment) {
        throw '프로그램 접속 주소를 http:// 또는 https://로 시작하도록 입력하세요.'
    }

    if ($PasswordWindow) {
        Add-Type -AssemblyName System.Windows.Forms
        Add-Type -AssemblyName System.Drawing
        [System.Windows.Forms.Application]::EnableVisualStyles()
        $passwordForm = New-Object System.Windows.Forms.Form
        $passwordInput = New-Object System.Windows.Forms.TextBox
        try {
            $passwordForm.Text = '아이엔씨 Gmail 연결'
            $passwordForm.ClientSize = New-Object System.Drawing.Size(630, 310)
            $passwordForm.Font = New-Object System.Drawing.Font('맑은 고딕', 11)
            $passwordForm.AutoScaleMode = [System.Windows.Forms.AutoScaleMode]::Dpi
            $passwordForm.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::FixedDialog
            $passwordForm.StartPosition = [System.Windows.Forms.FormStartPosition]::CenterScreen
            $passwordForm.MaximizeBox = $false
            $passwordForm.MinimizeBox = $false

            $senderLabel = New-Object System.Windows.Forms.Label
            $senderLabel.Text = "보내는 메일: $GmailAddress"
            $senderLabel.SetBounds(24, 22, 582, 32)
            $passwordForm.Controls.Add($senderLabel)

            $instructionLabel = New-Object System.Windows.Forms.Label
            $instructionLabel.Text = "방금 Google에서 만든 앱 비밀번호 16자리를 붙여넣으세요.`r`n이 메일 계정에서 만든 앱 비밀번호여야 합니다."
            $instructionLabel.SetBounds(24, 65, 582, 55)
            $passwordForm.Controls.Add($instructionLabel)

            $passwordInput.UseSystemPasswordChar = $true
            $passwordInput.MaxLength = 128
            $passwordInput.SetBounds(24, 133, 582, 34)
            $passwordForm.Controls.Add($passwordInput)

            $noticeLabel = New-Object System.Windows.Forms.Label
            $noticeLabel.Text = "일반 메일 로그인 비밀번호는 넣지 마세요.`r`n입력 내용은 이 컴퓨터에서만 사용하며 파일에 저장하지 않습니다."
            $noticeLabel.SetBounds(24, 181, 582, 54)
            $passwordForm.Controls.Add($noticeLabel)

            $completeButton = New-Object System.Windows.Forms.Button
            $completeButton.Text = '입력 완료'
            $completeButton.SetBounds(356, 251, 120, 36)
            $completeButton.Add_Click({
                if (($passwordInput.Text -replace '\s', '').Length -ne 16) {
                    [System.Windows.Forms.MessageBox]::Show($passwordForm, 'Google에서 만든 앱 비밀번호 16자리를 입력하세요.', '입력 확인', [System.Windows.Forms.MessageBoxButtons]::OK, [System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
                    $passwordInput.Focus() | Out-Null
                    return
                }
                $passwordForm.DialogResult = [System.Windows.Forms.DialogResult]::OK
            })
            $passwordForm.Controls.Add($completeButton)
            $passwordForm.AcceptButton = $completeButton

            $cancelButton = New-Object System.Windows.Forms.Button
            $cancelButton.Text = '취소'
            $cancelButton.DialogResult = [System.Windows.Forms.DialogResult]::Cancel
            $cancelButton.SetBounds(486, 251, 120, 36)
            $passwordForm.Controls.Add($cancelButton)
            $passwordForm.CancelButton = $cancelButton
            $passwordForm.Add_Shown({ $passwordForm.Activate(); $passwordInput.Focus() | Out-Null })

            if ($passwordForm.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) {
                throw '메일 연결 입력을 취소했습니다. 기존 프로그램은 그대로 유지됩니다.'
            }
            $securePassword = ConvertTo-SecureString -String $passwordInput.Text -AsPlainText -Force
        } finally {
            $passwordInput.Clear()
            $passwordForm.Dispose()
        }
    } else {
        $securePassword = Read-Host 'Google에서 만든 앱 비밀번호 (입력 내용은 표시되지 않습니다)' -AsSecureString
    }
    $plainPassword = ([Net.NetworkCredential]::new('', $securePassword).Password) -replace '\s', ''
    if ($plainPassword.Length -ne 16) { throw 'Google 앱 비밀번호 16자리를 입력하세요. Gmail 로그인 비밀번호를 입력하는 칸이 아닙니다.' }
    $env:GMAIL_ADDRESS = $GmailAddress
    $env:GMAIL_APP_PASSWORD = $plainPassword
    $plainPassword = $null
    $env:APP_BASE_URL = $AccessUrl.TrimEnd('/')
    $env:APPROVAL_EMAIL_ENABLED = 'true'
    $env:GRADLE_USER_HOME = Join-Path $backendDirectory '.gradle-user-home'
    if ($WaitForExistingBackend) {
        $setupDirectory = Join-Path $backendDirectory 'build/gmail-setup'
        New-Item -ItemType Directory -Path $setupDirectory -Force | Out-Null
        $readyMarker = Join-Path $setupDirectory ("ready-$PID.flag")
        [IO.File]::WriteAllText($readyMarker, 'READY')
        Write-Host '입력이 끝났습니다. 연결 준비 중입니다. 이 창을 열어둔 채 채팅에 입력을 마쳤다고 알려주세요.' -ForegroundColor Cyan
        while (Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue) { Start-Sleep -Seconds 2 }
    }
    Write-Host 'Gmail 결재 알림을 켜고 서버를 시작합니다. 이 창을 열어두세요.' -ForegroundColor Green
    Write-Host '직원별 수신 주소는 시스템관리 > 계정관리 > 수정 > 이메일에서 등록하세요.'
    & .\gradlew.bat bootRun --console=plain
    if ($LASTEXITCODE -ne 0) { throw '서버 시작에 실패했습니다. 위 오류 내용을 확인하세요.' }
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
} finally {
    foreach ($bindingName in $bindingNames) { [Environment]::SetEnvironmentVariable($bindingName, $previousBindings[$bindingName], 'Process') }
    if ($securePassword) { $securePassword.Dispose() }
    if ($readyMarker -and (Test-Path -LiteralPath $readyMarker)) { Remove-Item -LiteralPath $readyMarker }
    Pop-Location
}
