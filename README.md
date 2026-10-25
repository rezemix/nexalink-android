# NEXALINK для Android

Бесплатный VPN-клиент без регистрации и рекламы. Форк [v2rayNG](https://github.com/2dust/v2rayNG) на ядре [Xray](https://github.com/XTLS/Xray-core).

Скачать APK: <https://nexalink.online/dl/nexalink.apk> · сайт: <https://nexalink.online> · [релизы](https://github.com/rezemix/nexalink-android/releases)

Серверную часть (панель и агент для нод) можно развернуть самому: [rezemix/nexalink-panel](https://github.com/rezemix/nexalink-panel).

## Чем отличается от v2rayNG

- **Доступ сразу после установки.** Приложение само получает бесплатную подписку у панели по хэшу идентификатора устройства — без логина и настроек.
- **Вход по ссылке подписки.** Ссылка `https://sub.nexalink.online/…` открывает приложение и подключает доступ (App Links, `nexalink://install-sub`).
- **Российские сайты напрямую.** Пресет маршрутизации «Россия»: `geosite:category-ru` и `geoip:ru` мимо VPN, `domainStrategy: IPIfNonMatch`. Госуслуги и банковские приложения идут мимо VPN на уровне Android.
- **Блокировка рекламы.** Правило `geosite:category-ads-all` плюс расширенный список (AdGuard DNS filter + RU AdList), который приложение раз в сутки скачивает с панели.
- **Свой интерфейс.** Кнопка подключения, таймер, скорость, трафик и IP выхода, выбор сервера с флагами, режим «Авто».
- **Виджеты** 1×1 и 4×1, уведомление со скоростью и кнопками «Отключить» / «Переподключить».
- **«Мой доступ».** Ссылка устройства, отдельный доступ для другого устройства по QR-коду (iPhone, компьютер — через Happ), приглашение друзей.
- **Тихая синхронизация подписки** при открытии приложения без лишних переподключений VPN.

## Сборка

Нужны JDK 17 и Android SDK. Нативные библиотеки в репозиторий не входят (см. `.gitignore`), положите их перед сборкой:

- `V2rayNG/app/libs/libv2ray.aar` — сборка [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite);
- `V2rayNG/app/libs/arm64-v8a/libhev-socks5-tunnel.so` и `libhevsockstun.so` — [hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel).

```bash
cd V2rayNG
./gradlew assembleFdroidDebug -PABI_FILTERS=arm64-v8a
```

Вариант `playstore` использует пакет `online.nexalink.app`, вариант `fdroid` — `online.nexalink.app.fdroid`.

## Лицензия

GPL-3.0, как и у v2rayNG. Исходный README v2rayNG — в [README.v2rayNG.md](README.v2rayNG.md).
