# 오늘도 금주 — Android APK 시작 프로젝트

이 프로젝트는 기존 GitHub Pages 웹앱을 Android WebView에서 실행하는 **초기 시험용 프로젝트**입니다. 원본 `grumjoo` 저장소를 수정하지 않습니다.

## 빌드
1. 별도의 GitHub 저장소에 이 폴더의 파일을 업로드합니다.
2. Actions 탭에서 **Build Android APK** 워크플로를 실행합니다.
3. 빌드가 성공하면 `geumju-debug-apk` 아티팩트를 내려받아 APK를 테스트합니다.

또는 Android Studio에서 프로젝트를 열어 Debug APK를 빌드합니다.

## 중요: 지인 배포 전에 반드시 해결할 항목
- **푸시 알림**: 현재 웹 FCM/service worker 방식은 WebView에서 동일하게 동작하지 않을 수 있습니다. Android용 Firebase Messaging SDK와 토큰 등록/알림 설정 UI를 구현하고 실기기 테스트해야 합니다.
- **백업 다운로드**: 원본 웹앱은 Blob URL 다운로드를 사용합니다. WebView에서 이를 처리하는 네이티브 저장 기능이 아직 없습니다. JSON 백업 내보내기를 구현/검증하기 전에는 기록 보존이 보장되지 않습니다.
- **데이터 이전**: 기존 Chrome/PWA localStorage는 새 APK WebView로 자동 이전되지 않습니다. 기존 앱의 백업 JSON을 안전하게 보관한 후 가져오기로 복원해야 합니다.
- **개인정보**: 사용자별 기록은 WebView 내부 localStorage에 저장되지만 기기 분실/앱 삭제 시 소실될 수 있습니다.
- **릴리스 서명**: 이 프로젝트의 Actions 결과물은 debug APK입니다. 지인 배포용 정식 서명 키와 업데이트 체계를 따로 준비해야 합니다.
- **인터넷 필요**: 앱은 GitHub Pages URL을 열기 때문에 오프라인으로 작동하지 않습니다.

원본 웹앱의 데이터를 삭제하거나 앱을 제거하지 마세요. 실제 APK 설치 테스트와 기능 검증이 끝나기 전까지 기존 앱을 계속 사용하세요.
