$ErrorActionPreference = 'Stop'

$out = Join-Path $PSScriptRoot 'INC_공사통합관리_사용자매뉴얼_v1.0.docx'
$tmp = Join-Path $PSScriptRoot '.manual_docx_tmp'
if (Test-Path -LiteralPath $tmp) { Remove-Item -LiteralPath $tmp -Recurse -Force }
New-Item -ItemType Directory -Path $tmp, (Join-Path $tmp '_rels'), (Join-Path $tmp 'docProps'), (Join-Path $tmp 'word'), (Join-Path $tmp 'word\_rels') | Out-Null

function E([string]$value) { return [System.Security.SecurityElement]::Escape($value) }
function R([string]$text, [int]$size = 21, [bool]$bold = $false, [string]$color = '202124') {
  $b = if ($bold) { '<w:b/>' } else { '' }
  return "<w:r><w:rPr>$b<w:rFonts w:ascii=`"Aptos`" w:hAnsi=`"Aptos`" w:eastAsia=`"Malgun Gothic`"/><w:color w:val=`"$color`"/><w:sz w:val=`"$size`"/><w:szCs w:val=`"$size`"/></w:rPr><w:t xml:space=`"preserve`">$(E $text)</w:t></w:r>"
}
function P([string]$text, [string]$style = '', [int]$size = 21, [bool]$bold = $false, [string]$color = '202124', [string]$align = '', [int]$after = 100, [int]$before = 0) {
  $sty = if ($style) { "<w:pStyle w:val=`"$style`"/>" } else { '' }
  $jc = if ($align) { "<w:jc w:val=`"$align`"/>" } else { '' }
  return "<w:p><w:pPr>$sty$jc<w:spacing w:before=`"$before`" w:after=`"$after`" w:line=`"290`" w:lineRule=`"auto`"/></w:pPr>$(R $text $size $bold $color)</w:p>"
}
function Bullet([string]$text) {
  return "<w:p><w:pPr><w:ind w:left=`"480`" w:hanging=`"240`"/><w:spacing w:after=`"50`" w:line=`"280`"/></w:pPr>$(R '•' 21 $true '1F4E79')$(R "  $text" 21 $false '202124')</w:p>"
}
function PageBreak { return '<w:p><w:r><w:br w:type="page"/></w:r></w:p>' }
function Cell([string]$text, [int]$width, [string]$fill = 'FFFFFF', [bool]$bold = $false, [string]$color = '202124', [string]$align = 'left', [int]$size = 18) {
  return "<w:tc><w:tcPr><w:tcW w:w=`"$width`" w:type=`"dxa`"/><w:shd w:fill=`"$fill`"/><w:tcBorders><w:top w:val=`"single`" w:sz=`"6`" w:color=`"D9D9D9`"/><w:left w:val=`"single`" w:sz=`"6`" w:color=`"D9D9D9`"/><w:bottom w:val=`"single`" w:sz=`"6`" w:color=`"D9D9D9`"/><w:right w:val=`"single`" w:sz=`"6`" w:color=`"D9D9D9`"/></w:tcBorders><w:tcMar><w:top w:w=`"90`" w:type=`"dxa`"/><w:start w:w=`"110`" w:type=`"dxa`"/><w:bottom w:w=`"90`" w:type=`"dxa`"/><w:end w:w=`"110`" w:type=`"dxa`"/></w:tcMar><w:vAlign w:val=`"center`"/></w:tcPr><w:p><w:pPr><w:jc w:val=`"$align`"/><w:spacing w:after=`"0`"/></w:pPr>$(R $text $size $bold $color)</w:p></w:tc>"
}
function Table([string[]]$headers, [object[]]$rows, [int[]]$widths) {
  $grid = ($widths | ForEach-Object { "<w:gridCol w:w=`"$_`"/>" }) -join ''
  $headerCells = for ($i=0; $i -lt $headers.Count; $i++) { Cell $headers[$i] $widths[$i] '1F4E79' $true 'FFFFFF' 'center' 17 }
  $xml = "<w:tbl><w:tblPr><w:tblW w:w=`"0`" w:type=`"auto`"/><w:tblLayout w:type=`"fixed`"/></w:tblPr><w:tblGrid>$grid</w:tblGrid><w:tr><w:trPr><w:tblHeader/></w:trPr>$($headerCells -join '')</w:tr>"
  for ($r=0; $r -lt $rows.Count; $r++) {
    $fill = if ($r % 2 -eq 0) {'FFFFFF'} else {'F4F8FC'}
    $cells = for ($i=0; $i -lt $headers.Count; $i++) { Cell ([string]$rows[$r][$i]) $widths[$i] $fill $false '202124' ($(if ($i -eq 0 -and $headers.Count -le 4) {'center'} else {'left'})) 17 }
    $xml += "<w:tr>$($cells -join '')</w:tr>"
  }
  return $xml + '</w:tbl><w:p><w:pPr><w:spacing w:after="80"/></w:pPr></w:p>'
}
function Screen([string]$caption, [string]$pageTitle, [string[]]$tabs, [object[]]$cards, [object[]]$columns, [string]$note) {
  $xml = P "화면 예시  $caption" '' 20 $true '000000' '' 40 100
  $xml += '<w:tbl><w:tblPr><w:tblW w:w="9800" w:type="dxa"/><w:tblLayout w:type="fixed"/></w:tblPr><w:tblGrid><w:gridCol w:w="9800"/></w:tblGrid><w:tr>' + (Cell '아이엔씨 ERP   |   공사 통합관리' 9800 '1F4E79' $true 'FFFFFF' 'left' 18) + '</w:tr><w:tr><w:tc><w:tcPr><w:tcW w:w="9800" w:type="dxa"/><w:tcBorders><w:top w:val="single" w:sz="6" w:color="D9D9D9"/><w:left w:val="single" w:sz="6" w:color="D9D9D9"/><w:bottom w:val="single" w:sz="6" w:color="D9D9D9"/><w:right w:val="single" w:sz="6" w:color="D9D9D9"/></w:tcBorders><w:tcMar><w:top w:w="100" w:type="dxa"/><w:start w:w="110" w:type="dxa"/><w:bottom w:w="100" w:type="dxa"/><w:end w:w="110" w:type="dxa"/></w:tcMar></w:tcPr>'
  $xml += '<w:tbl><w:tblPr><w:tblLayout w:type="fixed"/></w:tblPr><w:tblGrid>' + (($tabs | ForEach-Object {'<w:gridCol w:w="' + [int](9400/$tabs.Count) + '"/>'}) -join '') + '</w:tblGrid><w:tr>'
  for ($i=0; $i -lt $tabs.Count; $i++) { $xml += Cell $tabs[$i] ([int](9400/$tabs.Count)) ($(if($i -eq 0){'D9EAF7'}else{'FFFFFF'})) ($i -eq 0) ($(if($i -eq 0){'1F4E79'}else{'5B6573'})) 'center' 15 }
  $xml += '</w:tr></w:tbl>'
  $xml += P $pageTitle '' 24 $true '202124' '' 60 100
  if ($cards.Count -gt 0) { $cw=[int](9400/$cards.Count); $xml += '<w:tbl><w:tblPr><w:tblLayout w:type="fixed"/></w:tblPr><w:tblGrid>' + (($cards | ForEach-Object {'<w:gridCol w:w="' + $cw + '"/>'}) -join '') + '</w:tblGrid><w:tr>'; foreach ($card in $cards) { $xml += Cell ($card -join "`n") $cw 'F4F8FC' $false '202124' 'left' 16 }; $xml += '</w:tr></w:tbl>' }
  if ($columns.Count -gt 0) { $cw=[int](9400/$columns.Count); $xml += '<w:tbl><w:tblPr><w:tblLayout w:type="fixed"/></w:tblPr><w:tblGrid>' + (($columns | ForEach-Object {'<w:gridCol w:w="' + $cw + '"/>'}) -join '') + '</w:tblGrid><w:tr>'; foreach ($column in $columns) { $xml += Cell ($column -join "`n") $cw 'FFFFFF' $false '202124' 'left' 15 }; $xml += '</w:tr></w:tbl>' }
  $xml += P $note '' 15 $false '5B6573' '' 40 50
  return $xml + '</w:tc></w:tr></w:tbl><w:p><w:pPr><w:spacing w:after="50"/></w:pPr></w:p>'
}

$body = ''
# Cover
$body += P '아이엔씨 공사 통합관리 시스템 사용자 매뉴얼' 'Title' 46 $true '000000' 'center' 260 2600
$body += P '프로젝트 등록부터 준공정산까지' '' 28 $false '5B6573' 'center' 80 0
$body += P '계약  실행예산  발주  기성  지급  준공정산 업무 흐름 안내' '' 22 $false '5B6573' 'center' 800 0
$body += Table @('문서 정보','내용') @(@('문서 버전','v1.0'),@('기준일','2026년 10월 28일'),@('대상 사용자','대표이사  팀장  현장담당자  관리 회계 담당자')) @(2200,7600)
$body += PageBreak

# 1
$body += P '1  이 매뉴얼을 사용하는 방법' 'Heading1' 32 $true '000000' '' 140 0
$body += P '이 문서는 실제 시스템에 표시되는 메뉴명과 입력 흐름을 기준으로 작성했습니다. 먼저 프로젝트를 선택한 뒤, 상단 통합현황과 프로젝트별 탭을 따라 업무를 진행하면 됩니다.' '' 21 $false '202124' '' 120 0
$body += Table @('표기','의미','사용 방법') @(@('화면 예시','실제 UI의 메뉴와 주요 정보 배치를 단순화한 구성도','화면에서 찾을 위치를 먼저 확인합니다.'),@('입력','사용자가 직접 작성하거나 선택하는 필드','금액, 사유, 담당자, 첨부파일을 빠짐없이 입력합니다.'),@('결재요청','대표 또는 결재권자 검토가 필요한 단계','저장 후 통합결재함에서 상태를 확인합니다.'),@('주의','금액 흐름과 이력 보존을 위한 업무 규칙','승인본을 덮어쓰지 않고 변경 차수 또는 변경안을 사용합니다.')) @(1600,4000,4200)
$body += P '전체 업무 흐름' 'Heading2' 25 $true '000000' '' 80 100
$body += Table @('순서','업무 단계','주요 산출물','확인 화면') @(@('1','프로젝트 등록','프로젝트 번호와 기본 일정','프로젝트 현황'),@('2','고객 계약','계약 공종과 계약금액','고객 견적 계약 관리'),@('3','세부실행예산','공종별 실행항목과 원가','세부실행예산'),@('4','업체 비교와 발주','선정업체와 발주품의','업체 비교견적'),@('5','기성과 지급','차수별 기성 및 지급결재','기성 및 지급 현황'),@('6','준공정산','최종 원가와 이익 및 결재','준공정산')) @(700,2200,3500,3400)
$body += PageBreak

# 2
$body += P '2  처음 시작하기' 'Heading1' 32 $true '000000' '' 140 0
$body += P '2 1  상단 메뉴와 프로젝트 선택' 'Heading2' 25 $true '000000' '' 80 0
$body += Screen '프로젝트 목록' '프로젝트 현황' @('프로젝트 현황','통합결재함','변경관리 이력') @() @(@('프로젝트명 프로젝트번호','성가수녀원 리뉴얼공사','P-2026-001'),@('고객 현장','성가수녀원','서울시 성북구'),@('일정 상태','2026-03-10 시작','공사진행')) '프로젝트명 또는 프로젝트번호로 검색한 뒤, 목록의 프로젝트 행을 선택하면 통합현황 상세로 이동합니다.'
$body += Bullet '프로젝트 현황에서 프로젝트 등록을 누르면 신규 프로젝트의 기본정보를 등록할 수 있습니다.'
$body += Bullet '프로젝트 목록의 프로젝트명 행을 누르면 해당 프로젝트의 통합현황과 세부 업무 탭이 열립니다.'
$body += Bullet '통합결재함은 여러 프로젝트의 결재를 한 화면에서 확인하는 전역 메뉴입니다.'
$body += Bullet '변경관리 이력은 먼저 프로젝트를 선택해야 해당 프로젝트의 차수를 조회하고 작성할 수 있습니다.'
$body += P '2 2  프로젝트 등록 시 필수 확인 항목' 'Heading2' 25 $true '000000' '' 80 100
$body += Table @('항목','입력 기준','업무상 의미') @(@('프로젝트명과 고객 발주처','계약서의 공식 명칭을 사용','목록과 결재문서의 식별 기준'),@('계약일 착공일 준공예정일','실제 계약 및 현장 일정 기준','프로젝트 진행상태 판단'),@('담당 팀장과 담당자','내부 담당자를 지정','현장 기록과 책임자 확인'),@('목표 이익률','계약 검토 시 합의된 비율','목표원가한도를 자동 산정')) @(2200,4000,4200)
$body += PageBreak

# 3
$body += P '3  프로젝트 통합현황 읽기' 'Heading1' 32 $true '000000' '' 120 0
$body += P '프로젝트를 선택하면 계약부터 지급까지의 현재 승인 기준 금액을 한 화면에서 확인할 수 있습니다. 수주 및 목표 현황과 실제 자금 집행 현황을 함께 읽는 것이 핵심입니다.' '' 21 $false '202124' '' 90 0
$body += Screen '통합현황' '성가수녀원 리뉴얼공사  공사진행' @('고객 견적 계약 관리','세부실행예산','업체 비교견적','기성 및 지급 현황','준공정산') @(@('현재 계약금액','220,000,000 원','현재 승인 계약 공급가액'),@('목표 원가한도','121,000,000 원','목표 이익률 기준'),@('예상 이익 이익률','103,780,000 원  47.2%','계약금액 - 예상 최종원가')) @(@('실행과 발주','현재 실행예산 6,020,000 원','현재 발주금액 110,200,000 원'),@('기성과 지급','누적 기성금액 80,000,000 원','실제 지급금액 70,000,000 원'),@('잔액','총 미지급 40,200,000 원','승인 후 미지급 10,000,000 원')) '모든 집계는 현재 승인 차수를 기준으로 표시됩니다. 이전 차수의 기록은 보존되지만 현재 총액에는 포함되지 않습니다.'
$body += Table @('지표','확인 방법','주의점') @(@('목표원가한도','계약 공급가액과 목표 이익률로 자동 계산','계약 변경 시 함께 검토합니다.'),@('예상 최종원가','확정 발주와 미발주 실행예산 및 직접 정산비 반영','예상 이익을 판단하는 기준입니다.'),@('미기성금액','현재 발주금액에서 승인 기성누계를 뺀 금액','아직 승인되지 않은 공정 잔액입니다.'),@('승인 후 미지급','승인 기성누계에서 실제 지급누계를 뺀 금액','지급결재가 필요한 금액입니다.')) @(1900,4600,3900)
$body += PageBreak

# 4
$body += P '4  고객 견적과 계약 관리' 'Heading1' 32 $true '000000' '' 100 0
$body += Screen '고객 견적 계약 관리' '공급가액 기준 계약금액과 목표이익률' @('고객 견적 계약 관리','세부실행예산','업체 비교견적') @(@('계약 총 공급가액','220,000,000 원','최초 계약 및 승인 변경 반영'),@('목표 이익','99,000,000 원','목표 이익률 45 퍼센트'),@('목표원가한도','121,000,000 원','계약 공급가액 기준')) @(@('계약 공종표','공종','견적 제출 공급가액','최종 계약 공급가액'),@('자동 계산','VAT 10 퍼센트','VAT 포함 총액','합계'),@('증빙','견적서 첨부','계약서 첨부','계약 변경안 작성')) '계약 공급가액을 입력하면 VAT가 자동 계산됩니다. 승인된 최초 금액은 수정하지 말고 계약 변경안을 작성합니다.'
$body += P '처리 순서' 'Heading2' 25 $true '000000' '' 60 80
$body += Bullet '공종별 견적 제출 금액과 최종 계약 공급가액을 입력합니다.'
$body += Bullet '목표 이익률을 확인해 목표원가한도가 현실적인지 검토합니다.'
$body += Bullet '견적서와 계약서를 구분해 첨부한 뒤 계약 저장을 누릅니다.'
$body += Bullet '계약금액 또는 계약 공종을 바꿔야 하면 계약 변경안 작성을 사용하고, 변경관리 이력에서 차수를 관리합니다.'
$body += PageBreak

# 5
$body += P '5  세부실행예산 작성과 결재요청' 'Heading1' 32 $true '000000' '' 100 0
$body += Screen '세부실행예산' '최초 승인본 세부실행예산 항목' @('세부실행예산','업체 비교견적','계약 발주 이력') @(@('계약 공급가액','220,000,000 원','고객 계약 기준'),@('목표원가한도','121,000,000 원','목표이익률 기준'),@('세부실행예산 총합계','6,020,000 원','현재 작성 차수')) @(@('실행항목ID별 집계','EX-001','인테리어공사','외주비 폐기물 인건비'),@('세부 항목','원가구분','규격 단위 수량 단가','금액과 비고'),@('완료 작업','행 추가 또는 행 복사','세부실행예산 저장','결재요청')) '발주가 필요한 항목은 승인된 실행예산에서만 업체 비교견적의 선택 대상으로 표시됩니다.'
$body += Table @('입력 항목','작성 기준','자동 반영') @(@('실행항목ID','같은 발주 단위는 같은 ID로 입력','ID별 금액이 자동 집계됩니다.'),@('원가구분','외주비 폐기물 인건비 등 실제 성격 선택','공종별 원가분석에 반영됩니다.'),@('수량과 단가','부가세 포함 여부를 혼동하지 않도록 기준 통일','금액이 자동 계산됩니다.'),@('결재 첨부파일','산출근거와 견적자료를 첨부','결재권자가 검토할 근거가 됩니다.')) @(1900,4600,3900)
$body += P '주의  실행예산을 저장했다고 발주할 수 있는 것은 아닙니다. 결재요청 후 승인된 실행예산 항목만 업체 비교견적 화면에서 선택됩니다.' '' 20 $true '1F4E79' '' 80 40
$body += PageBreak

# 6
$body += P '6  업체 비교견적과 발주품의' 'Heading1' 32 $true '000000' '' 100 0
$body += Screen '업체 비교견적' '승인 실행예산 기준 업체 비교와 발주품의' @('업체 비교견적','계약 발주 이력','기성 및 지급 현황') @(@('실행항목ID','EX-001','승인된 실행예산 항목'),@('승인 실행예산','6,020,000 원','롤업 금액 기준'),@('발주품의 상태','미요청','선정 후 결재요청')) @(@('비교견적 입력','업체명','견적금액','공사기간과 결제조건'),@('선정','한 업체 선택','선정사유 입력','견적서 파일 첨부'),@('발주 연결','비교견적 저장','발주품의 요청','통합결재 승인 후 발주 확정')) '발주품의 요청 후에는 비교견적을 임의로 바꿀 수 없습니다. 선정되지 않은 업체는 비교 이력으로 남고 발주 및 지급에는 연결되지 않습니다.'
$body += P '처리 순서' 'Heading2' 25 $true '000000' '' 60 80
$body += Bullet '실행항목ID 선택 목록에서 승인된 실행예산 항목을 고릅니다.'
$body += Bullet '업체별 견적금액, 공사기간, 결제조건을 입력하고 비교견적을 저장합니다.'
$body += Bullet '선정할 업체 한 곳을 선택하고 선정사유를 작성합니다.'
$body += Bullet '발주품의 요청을 누른 뒤 통합결재함에서 승인되면 선정업체 기준으로 발주가 확정됩니다.'
$body += PageBreak

# 7
$body += P '7  기성 및 지급 현황' 'Heading1' 32 $true '000000' '' 100 0
$body += Screen '기성 및 지급 현황' '발주별 기성 등록과 실제 지급 결재' @('기성 및 지급 현황','계약 발주 이력','준공정산') @(@('현재 발주금액','110,200,000 원','승인된 현재 차수 발주'),@('누적 기성금액','80,000,000 원','승인 기성 누계'),@('실제 지급금액','70,000,000 원','실제 송금 완료 누계')) @(@('기성 등록','발주 선택','차수와 청구금액 입력','기성 결재요청'),@('지급 요청','승인 후 미지급 발주 선택','지급결재 요청','결재 승인 후 실제 지급 반영'),@('잔액 확인','미기성금액','승인 후 미지급','총 미지급 잔액')) '현재 승인 차수의 발주만 집계됩니다. 이전 차수 발주와 기성 지급 기록은 보존되며 현재 차수 총액에서는 분리됩니다.'
$body += Table @('구분','뜻','업무 판단') @(@('미기성금액','현재 발주금액 - 승인 기성누계','공정이 끝나지 않았거나 기성 승인이 남은 금액'),@('승인 후 미지급','승인 기성누계 - 실제 지급누계','지급결재 대상 금액'),@('총 미지급 잔액','현재 발주금액 - 실제 지급누계','향후 집행해야 할 전체 발주 잔액')) @(2200,3900,4300)
$body += PageBreak

# 8
$body += P '8  통합결재함 처리' 'Heading1' 32 $true '000000' '' 100 0
$body += Screen '통합결재함' '실행예산 변경 차수 발주 기성 정산 결재를 한 화면에서 처리' @('통합결재함','프로젝트 현황','변경관리 이력') @() @(@('필터','전체 상태','결재대기 승인 반려 승인취소','전체 유형'),@('결재 목록','요청일','프로젝트와 유형','제목 요청금액 증감 상태'),@('결재 상세','요청 내용과 첨부 확인','승인 또는 반려','반려 의견 입력')) '목록에는 순번과 요청 시간이 분 단위로 표시됩니다. 결재 후에는 관련 프로젝트의 금액과 상태가 자동 갱신됩니다.'
$body += P '결재 처리 기준' 'Heading2' 25 $true '000000' '' 60 80
$body += Table @('유형','승인 전 확인','승인 후 결과') @(@('실행예산','산출근거, 목표원가한도, 첨부파일','업체 비교견적 선택 가능'),@('계약 실행예산 변경','증감사유와 변경 차수','현재 승인 차수 금액으로 집계'),@('발주','선정업체, 견적 대비 차이, 선정사유','선정업체 발주 확정'),@('기성 및 지급','발주 잔액, 기성 금액, 지급 대상','기성 또는 실제 지급 누계 갱신'),@('준공정산','미지급 사유, 최종 원가와 이익','정산완료 처리')) @(2200,4700,3800)
$body += PageBreak

# 9
$body += P '9  계약 변경 차수와 이력 관리' 'Heading1' 32 $true '000000' '' 100 0
$body += Screen '계약 변경 차수 관리' '계약금액 또는 계약 공종 변경' @('변경관리 이력','프로젝트 현황','통합결재함') @() @(@('대상 프로젝트','P-2026-001','성가수녀원 리뉴얼공사'),@('새 차수 작성','차수 제목','변경 사유','새 차수 작성'),@('관리 원칙','최초 금액 보존','승인 증감액 합산','현재금액 자동 계산')) '세부실행예산은 세부실행예산 메뉴에서 별도로 작성하고 결재합니다. 계약 변경 차수 화면은 계약금액과 계약 공종 변경만 관리합니다.'
$body += Bullet '최초 승인된 금액을 직접 덮어쓰지 않습니다. 새 차수를 작성해 변경 사유와 증감액을 남깁니다.'
$body += Bullet '현재 계약금액, 현재 실행예산, 현재 발주금액은 각각 승인된 증감 이력의 합으로 계산됩니다.'
$body += Bullet '현재 차수가 변경되면 이전 차수의 발주, 기성, 지급은 이력으로 보존되고 현재 집계에서는 분리될 수 있습니다.'
$body += Bullet '프로젝트별 변경 내역이 필요하면 프로젝트를 선택한 뒤 변경관리 이력 메뉴를 엽니다.'
$body += PageBreak

# 10
$body += P '10  현장 작업일지와 현장 회의록' 'Heading1' 32 $true '000000' '' 100 0
$body += P '현장 기록은 별도 결재 없이 프로젝트 이력으로 관리합니다. 공사 진행 중에는 작업일지로 일일 현황을 남기고, 협의 사항은 회의록과 후속조치 항목으로 추적합니다.' '' 21 $false '202124' '' 70 0
$body += Screen '현장 기록' '현장 작업일지와 현장 회의록' @('현장 작업일지','현장 회의록','기성 및 지급 현황') @() @(@('작업일지','작업일자와 날씨','투입 인원과 장비','작업 내용과 특이사항'),@('회의록','회의 일시 장소 주제','참석자와 논의 결정사항','후속조치 담당자 기한 상태'),@('첨부','현장 사진','도면과 관련문서','저장 후 파일 첨부')) '정산완료 프로젝트는 현장 작업일지와 회의록이 조회 전용으로 전환됩니다.'
$body += P '기록 품질 기준' 'Heading2' 25 $true '000000' '' 60 80
$body += Bullet '작업일지는 실제 작업일에 작성하고, 진행률이나 이슈가 있으면 구체적인 위치와 내용을 남깁니다.'
$body += Bullet '회의록에는 결정사항과 후속조치를 분리해 기록하고 담당자와 기한을 반드시 지정합니다.'
$body += Bullet '사진, 변경도면, 협의문 등 근거자료는 해당 기록을 저장한 뒤 첨부합니다.'
$body += PageBreak

# 11
$body += P '11  준공정산과 최종결재' 'Heading1' 32 $true '000000' '' 100 0
$body += Screen '준공정산' '계약 실행예산 발주 기성 지급 누계와 최종 손익' @('준공정산','기성 및 지급 현황','통합결재함') @(@('최종 계약금액','220,000,000 원','최초 금액과 승인 변경 반영'),@('최종 발주금액','110,200,000 원','발주 및 집행 현황'),@('최종 이익률','49.91%','최종 계약금액 대비')) @(@('집계 확인','계약 및 매출 현황','실행예산 현황','발주 기성 지급 현황'),@('직접 정산비 입력','미수금','현장경비','인건비와 준공 세금'),@('완료 조건','미지급 잔액 확인','사유와 정산 비고','준공정산 최종 결재요청')) '미지급 잔액이 남아 있으면 강제 완료 확인과 미지급 처리 사유가 필요합니다. 모든 조건을 충족하면 최종결재를 요청합니다.'
$body += Table @('정산 확인 항목','확인 내용','결재 전 조치') @(@('최종 계약금액','최초 계약과 승인된 계약 변경 반영 여부','누락된 계약변경이 있으면 먼저 차수 처리'),@('최종 원가','최종 발주금액과 현장경비 인건비 세금 반영 여부','직접 정산비를 실제 집행액으로 입력'),@('미지급 잔액','발주 기준 실제 지급 전 남은 금액','지급 완료 또는 강제 완료 사유 작성'),@('최종 이익','최종 계약금액 - 최종 원가','예상 이익과 차이가 큰 경우 원인 확인')) @(2200,4700,3800)
$body += PageBreak

# 12
$body += P '12  업무 규칙과 자주 묻는 질문' 'Heading1' 32 $true '000000' '' 100 0
$body += P '반드시 지키는 원칙' 'Heading2' 25 $true '000000' '' 50 0
$body += Table @('원칙','설명') @(@('최초 승인 금액 보존','승인된 최초 계약 실행예산 발주금액은 직접 수정하지 않습니다. 변경은 별도 차수 또는 변경 이력으로 남깁니다.'),@('결재와 실제 지급 분리','기성 승인과 실제 송금은 다른 단계입니다. 승인된 기성 중 실제 지급이 남으면 승인 후 미지급으로 관리합니다.'),@('발주 가능 항목 제한','업체 비교견적과 발주품의는 승인된 실행예산 항목만 진행할 수 있습니다.'),@('현재 차수 기준 집계','이전 차수의 기록은 보존하되, 현재 프로젝트 통합현황은 현재 승인 차수 기준으로 표시합니다.'),@('정산완료 후 조회 전용','정산완료 프로젝트는 기록 보존을 우선하며 신규 입력과 변경이 제한될 수 있습니다.')) @(2500,7200)
$body += P '자주 묻는 질문' 'Heading2' 25 $true '000000' '' 50 80
$body += Table @('질문','답변') @(@('저장했는데 업체 비교견적에서 항목이 보이지 않습니다.','세부실행예산을 결재요청하고 승인받았는지 확인합니다. 승인된 항목만 선택할 수 있습니다.'),@('발주금액과 기성금액이 왜 다릅니까.','발주는 약정 금액이고 기성은 공정에 따라 승인된 청구 누계입니다. 지급은 실제 송금 누계이므로 세 값은 서로 다를 수 있습니다.'),@('계약금액을 바꿔야 합니다.','기존 계약을 직접 수정하지 말고 계약 변경안과 변경 차수를 작성해 결재를 받습니다.'),@('정산 결재요청 버튼이 비활성화되어 있습니다.','미지급 잔액, 필수 사유, 강제 완료 확인, 정산비 입력값을 확인한 뒤 다시 시도합니다.')) @(4200,5500)
$body += P '문서 끝' '' 16 $false '5B6573' 'center' 0 120

$docXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$body<w:sectPr><w:pgSz w:w="12240" w:h="15840"/><w:pgMar w:top="936" w:right="1008" w:bottom="840" w:left="1008" w:header="720" w:footer="720" w:gutter="0"/></w:sectPr></w:body></w:document>
"@
$styles = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Aptos" w:hAnsi="Aptos" w:eastAsia="Malgun Gothic"/><w:sz w:val="21"/></w:rPr></w:rPrDefault></w:docDefaults><w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style><w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/></w:style><w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="Heading 1"/><w:basedOn w:val="Normal"/><w:qFormat/></w:style><w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="Heading 2"/><w:basedOn w:val="Normal"/><w:qFormat/></w:style></w:styles>
'@
$contentTypes = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/><Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/></Types>
'@
$rels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>
'@
$docRels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>
'@
$core = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:title>아이엔씨 공사 통합관리 시스템 사용자 매뉴얼</dc:title><dc:creator>INC</dc:creator><dcterms:created xsi:type="dcterms:W3CDTF">2026-10-28T00:00:00Z</dcterms:created></cp:coreProperties>
'@
$app = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"><Application>INC ERP</Application></Properties>
'@
[System.IO.File]::WriteAllText((Join-Path $tmp '[Content_Types].xml'), $contentTypes, [Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $tmp '_rels\.rels'), $rels, [Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $tmp 'docProps\core.xml'), $core, [Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $tmp 'docProps\app.xml'), $app, [Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $tmp 'word\document.xml'), $docXml, [Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $tmp 'word\styles.xml'), $styles, [Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $tmp 'word\_rels\document.xml.rels'), $docRels, [Text.Encoding]::UTF8)
if (Test-Path -LiteralPath $out) { Remove-Item -LiteralPath $out -Force }
[System.IO.Compression.ZipFile]::CreateFromDirectory($tmp, $out)
Remove-Item -LiteralPath $tmp -Recurse -Force
Write-Output $out
