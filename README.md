# CoffeeStock – Android Offline-First Inventory Application

CoffeeStock là ứng dụng Android quản lý kho cà phê theo mô hình Offline-First. Ứng dụng sử dụng Room để lưu dữ liệu cục bộ, ViewModel và Repository để tổ chức luồng dữ liệu, Retrofit để giao tiếp với REST API và WorkManager để thực hiện đồng bộ nền.

Tài liệu này hướng dẫn thiết lập môi trường, chạy máy chủ Node.js, kết nối Android với máy chủ bằng **ADB Reverse**, chạy ứng dụng trên Android Emulator và build file APK.

## 1. Công nghệ sử dụng

### Android

- Kotlin.
- Jetpack Compose và Material 3.
- Room và SQLite để lưu dữ liệu cục bộ.
- ViewModel, StateFlow và Kotlin Coroutines.
- Retrofit, Gson và OkHttp để gọi REST API.
- WorkManager để lập lịch đồng bộ nền.

### Server

- Node.js và Express.
- SQLite thông qua `better-sqlite3`.
- REST API.

## 2. Cấu trúc dự án

```text
CoffeeStock/
├── app/                         # Ứng dụng Android
│   └── src/main/java/com/example/coffeestock/
│       ├── data/
│       │   ├── local/            # Room, DAO, Entity
│       │   ├── remote/           # Retrofit, API và DTO
│       │   └── repository/       # Repository
│       ├── domain/               # Model nghiệp vụ
│       ├── sync/                 # WorkManager và xử lý đồng bộ
│       ├── ui/                   # Giao diện Jetpack Compose
│       ├── viewmodel/            # ViewModel
│       └── MainActivity.kt
├── server/                       # REST API Node.js
│   ├── src/
│   │   ├── config/
│   │   ├── database/
│   │   ├── middleware/
│   │   └── modules/
│   ├── data/                     # Database SQLite được tạo khi chạy
│   ├── .env.example
│   └── package.json
├── gradle/                       # Gradle Wrapper và version catalog
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## 3. Yêu cầu môi trường

Cài đặt các công cụ sau trên máy tính:

| Công cụ | Yêu cầu |
|---|---|
| Android Studio | Phiên bản hỗ trợ Android Gradle Plugin 9.4.1 |
| JDK | JDK 25 theo cấu hình Gradle Daemon của dự án |
| Android SDK Platform | API 37 (compileSdk và targetSdk của dự án) |
| Android SDK Platform-Tools | Có lệnh `adb` |
| Node.js | Node.js LTS, khuyến nghị Node.js 22 LTS |
| npm | Đi kèm với Node.js |

Dự án có Gradle Wrapper nên không cần cài Gradle riêng. Lần build đầu tiên cần Internet để Gradle tải Gradle distribution và các thư viện phụ thuộc.

Trong Android Studio, mở **Tools → SDK Manager** để cài Android SDK Platform 37 và Android SDK Platform-Tools nếu máy chưa có.

Nếu Android Studio không tự tìm thấy JDK 25, vào **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**, chọn Gradle JDK là JDK 25. Cấu hình `gradle/gradle-daemon-jvm.properties` của dự án cũng yêu cầu Gradle Daemon chạy bằng Java 25; dự án đã khai báo Foojay resolver để hỗ trợ tải JDK phù hợp khi cần.

Kiểm tra công cụ trong PowerShell:

```powershell
java -version
node -v
npm -v
adb version
```

Nếu PowerShell không nhận lệnh `adb`, mở Terminal tích hợp trong Android Studio hoặc thêm thư mục `platform-tools` trong Android SDK vào biến môi trường `PATH`.

## 4. Cấu hình máy chủ

### 4.1. Mở thư mục server

Mở PowerShell tại thư mục gốc `CoffeeStock`, sau đó chạy:

```powershell
cd server
```

### 4.2. Cài đặt thư viện Node.js

```powershell
npm install
```

### 4.3. Tạo file môi trường `.env`

Tạo `.env` từ file mẫu:

```powershell
Copy-Item .env.example .env
```

Mở `server/.env` và cấu hình:

```env
PORT=8080
HOST=0.0.0.0
API_KEY=coffeestock-dev-key
DATABASE_PATH=./data/coffeestock.db
```

**Quan trọng:** Giá trị `API_KEY` trong `server/.env` phải giống hoàn toàn với giá trị `COFFEESTOCK_API_KEY` trong file `gradle.properties` ở thư mục gốc. Cấu hình mẫu hiện tại của dự án sử dụng `coffeestock-dev-key` cho môi trường phát triển/demo. Nếu thay đổi API Key ở một nơi, phải cập nhật nơi còn lại và build lại ứng dụng Android.

Không đưa file `server/.env` thật lên GitHub. Chỉ đưa `server/.env.example` lên repository.

### 4.4. Khởi động server

Vẫn ở thư mục `server`, chạy:

```powershell
npm start
```

Khi khởi động thành công, terminal hiển thị thông báo tương tự:

```text
------------------------------------
CoffeeStock API is running
Address: http://localhost:8080
Health: http://localhost:8080/health
------------------------------------
```

Giữ cửa sổ terminal này mở trong suốt quá trình sử dụng ứng dụng.

Server tự tạo thư mục database nếu chưa tồn tại và khởi tạo cấu trúc SQLite khi khởi động lần đầu. Database mặc định nằm tại `server/data/coffeestock.db`.

### 4.5. Kiểm tra server

Mở trình duyệt trên máy tính và truy cập:

```text
http://localhost:8080/health
```

Hoặc mở PowerShell khác và chạy:

```powershell
Invoke-RestMethod http://localhost:8080/health
```

Kết quả mong đợi:

```json
{
  "status": "ok",
  "service": "CoffeeStock API",
  "version": "1.0.0"
}
```

Endpoint `/health` không yêu cầu API Key. Chỉ tiếp tục chạy Android sau khi kiểm tra được server.

## 5. Cấu hình Android kết nối server bằng ADB Reverse

**CoffeeStock sử dụng ADB Reverse để chuyển tiếp cổng 8080 từ Android Emulator về máy tính đang chạy server.** Đây là bước cần thực hiện trước khi kiểm tra chức năng gọi API/đồng bộ.

### 5.1. Cấu hình địa chỉ API

Mở file `gradle.properties` ở thư mục gốc dự án và kiểm tra hai dòng:

```properties
COFFEESTOCK_API_BASE_URL=http://127.0.0.1:8080/
COFFEESTOCK_API_KEY=coffeestock-dev-key
```

Địa chỉ `127.0.0.1:8080` được sử dụng cùng với ADB Reverse ở bước tiếp theo. Không đổi sang `10.0.2.2` khi đang làm theo quy trình ADB Reverse này.

### 5.2. Khởi động Android Emulator

1. Mở Android Studio.
2. Chọn **Tools → Device Manager**.
3. Tạo hoặc khởi động một Android Virtual Device (AVD).
4. Chờ Emulator khởi động hoàn tất.

### 5.3. Kiểm tra thiết bị ADB

Mở một terminal mới trên máy tính và chạy:

```powershell
adb devices
```

Ví dụ kết quả:

```text
List of devices attached
emulator-5554    device
```

Thiết bị phải có trạng thái `device`. Nếu danh sách trống, hãy mở Emulator, kiểm tra Android SDK Platform-Tools và chạy lại lệnh.

### 5.4. Thiết lập ADB Reverse

Chạy lệnh:

```powershell
adb reverse tcp:8080 tcp:8080
```

Kiểm tra quy tắc chuyển tiếp:

```powershell
adb reverse --list
```

Sau khi thiết lập, ứng dụng Android gọi `http://127.0.0.1:8080/` sẽ được chuyển tiếp đến cổng `8080` trên máy tính đang chạy server.

Nếu có nhiều thiết bị Android cùng kết nối, chỉ định đúng thiết bị bằng serial, ví dụ:

```powershell
adb -s emulator-5554 reverse tcp:8080 tcp:8080
```

Nếu Emulator khởi động lại hoặc kết nối ADB bị ngắt, kiểm tra và thực hiện lại lệnh ADB Reverse trước khi đồng bộ.

## 6. Build và chạy ứng dụng Android

### Cách 1: Chạy bằng Android Studio

1. Mở Android Studio.
2. Chọn **Open** và chọn thư mục gốc `CoffeeStock` (thư mục chứa `settings.gradle.kts`).
3. Chờ Gradle Sync hoàn tất. Nếu vừa sửa `gradle.properties`, chọn **File → Sync Project with Gradle Files**.
4. Khởi động Android Emulator.
5. Đảm bảo server đang chạy và đã thực hiện ADB Reverse theo Mục 5.
6. Chọn cấu hình chạy `app` và chọn Emulator làm thiết bị đích.
7. Nhấn **Run** để build, cài đặt và mở ứng dụng.

### Cách 2: Build bằng PowerShell

Mở PowerShell tại thư mục gốc `CoffeeStock`:

```powershell
.\gradlew.bat clean assembleDebug
```

Nếu build thành công, APK debug được tạo tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Để cài APK lên thiết bị đang kết nối ADB:

```powershell
.\gradlew.bat installDebug
```

Có thể cài trực tiếp APK bằng ADB:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 7. Kiểm tra chức năng đồng bộ

Thực hiện theo đúng thứ tự:

1. Server Node.js đang chạy tại cổng 8080.
2. Android Emulator đang hoạt động và được ADB nhận diện.
3. ADB Reverse đã được thiết lập bằng `adb reverse tcp:8080 tcp:8080`.
4. Ứng dụng CoffeeStock đã được build và mở trên Emulator.
5. Thực hiện thao tác dữ liệu trên giao diện, sau đó nhấn **Đồng bộ ngay** để yêu cầu đồng bộ.
6. Quan sát trạng thái phiếu trên giao diện và log của ứng dụng.

Để lọc log đồng bộ, mở terminal và chạy:

```powershell
adb logcat -s CoffeeStockSync
```

Một số log có thể gặp:

| Log | Ý nghĩa |
|---|---|
| `SYNC_START` | WorkManager bắt đầu thực hiện tác vụ đồng bộ |
| `SYNC_RESULT=SUCCESS` | Tác vụ đồng bộ hoàn thành thành công |
| `SYNC_RESULT=RETRY` | Tác vụ gặp lỗi có thể thử lại |
| `SYNC_RESULT=FAILED` | Tác vụ đồng bộ thất bại |
| `SYNC_RESULT=ERROR` | Có ngoại lệ xảy ra trong quá trình đồng bộ; xem phần stack trace đi kèm |

Khi cần xác nhận server nhận request, quan sát terminal đang chạy `npm start` hoặc kiểm tra dữ liệu SQLite phía server.

## 8. Cấu hình khi sử dụng điện thoại Android thật

Nếu chạy trên điện thoại Android thật qua USB:

1. Bật **Developer options** và **USB debugging** trên điện thoại.
2. Kết nối điện thoại với máy tính bằng USB và chấp nhận hộp thoại cho phép USB debugging.
3. Kiểm tra thiết bị:

```powershell
adb devices
```

4. Thiết lập chuyển tiếp cổng:

```powershell
adb reverse tcp:8080 tcp:8080
```

5. Giữ cấu hình `COFFEESTOCK_API_BASE_URL=http://127.0.0.1:8080/` và build/cài đặt lại ứng dụng nếu cần.

ADB Reverse chỉ hoạt động khi thiết bị còn kết nối ADB với máy tính. Nếu chuyển sang chạy qua Wi-Fi mà không dùng ADB Reverse, cần cấu hình địa chỉ IP LAN của máy tính và cho phép cổng 8080 qua firewall; đó không phải quy trình mặc định được hướng dẫn trong tài liệu này.

## 9. Xử lý lỗi thường gặp

| Hiện tượng | Cách kiểm tra/xử lý |
|---|---|
| `adb` không được nhận diện | Cài Android SDK Platform-Tools hoặc thêm thư mục `platform-tools` vào PATH. |
| `adb devices` không có thiết bị | Khởi động Emulator, kiểm tra USB debugging nếu dùng điện thoại, sau đó chạy lại `adb devices`. |
| Retrofit báo `Connection refused` hoặc không kết nối được | Kiểm tra server có chạy ở cổng 8080 không; chạy lại `adb reverse tcp:8080 tcp:8080`; kiểm tra Base URL là `http://127.0.0.1:8080/`. |
| `adb reverse --list` không có cổng 8080 | Chạy lại `adb reverse tcp:8080 tcp:8080`. |
| HTTP 401 hoặc `INVALID_API_KEY` | Đảm bảo `API_KEY` trong `server/.env` trùng với `COFFEESTOCK_API_KEY` trong `gradle.properties`; sau khi đổi key phải khởi động lại server và build lại Android. |
| HTTP 404 | Kiểm tra endpoint API và Base URL; Base URL phải kết thúc bằng dấu `/`. |
| HTTP 500 | Xem lỗi tại terminal server để xác định nguyên nhân phía API/database. |
| Gradle Sync hoặc build thất bại | Kiểm tra JDK 25, Android SDK Platform 37, kết nối Internet và thông báo lỗi đầu tiên trong Build Output. |
| WorkManager chưa chạy | Kiểm tra thiết bị có kết nối mạng, server đang chạy và ADB Reverse còn hiệu lực; sau đó nhấn Đồng bộ ngay. |

## 10. Các lệnh thường dùng

| Mục đích | Lệnh |
|---|---|
| Cài thư viện server | `cd server; npm install` |
| Chạy server | `npm start` (trong thư mục `server`) |
| Kiểm tra server | `Invoke-RestMethod http://localhost:8080/health` |
| Kiểm tra thiết bị | `adb devices` |
| Tạo ADB Reverse | `adb reverse tcp:8080 tcp:8080` |
| Kiểm tra ADB Reverse | `adb reverse --list` |
| Xem log đồng bộ | `adb logcat -s CoffeeStockSync` |
| Build APK | `.\gradlew.bat clean assembleDebug` |
| Cài ứng dụng | `.\gradlew.bat installDebug` |

## 11. Lưu ý khi nộp và chia sẻ source code

- Đưa mã nguồn Android, Gradle Wrapper, mã nguồn server, `package-lock.json`, `server/.env.example` và README lên repository Git.
- Không commit `server/.env`, database SQLite phát sinh, `local.properties`, thư mục `node_modules`, `.gradle` hoặc thư mục build.
- Người nhận source cần tự tạo `server/.env` theo Mục 4 và cấu hình API Key trùng với `gradle.properties`.
- Quy trình kết nối mặc định trong tài liệu sử dụng Android Emulator/thiết bị Android có ADB và lệnh `adb reverse tcp:8080 tcp:8080`.
- API Key mẫu chỉ phục vụ môi trường học tập và demo cục bộ; không sử dụng khóa này cho môi trường triển khai công khai.
