# Запуск RoastPoint целиком в Docker

Java 25 и Maven устанавливать на компьютер не нужно: Dockerfile собирает
приложение из исходников, выполняет тесты и помещает JAR в отдельный образ.
Нужны Docker Desktop в режиме Linux containers и доступ к Docker Hub/Maven Central.

## Быстрый запуск
1. Распакуйте архив и откройте PowerShell в папке RoastPointBackend, где compose.yaml.
2. Запустите Docker Desktop и дождитесь готовности движка.
3. Остановите Java-приложение в IntelliJ, если оно занимает порт 8080.
4. Выполните:

    $env:DEMO_PASSWORD='Coffee-Demo-2026!'
    docker compose up --build -d
    docker compose logs -f app

Первый запуск скачивает образы и зависимости, поэтому может занять несколько минут.
Дождитесь строки Started RoastPointApplication. Ctrl+C завершает просмотр логов,
контейнеры продолжают работать. БД создаётся контейнером, таблицы — приложением.
API требует входа: cashier1, barista1 или admin1 и пароль из DEMO_PASSWORD.
Переменная применяется только к исходным демонстрационным аккаунтам, один раз.
Задайте её до первого запуска (минимум 12 символов, максимум 72 байта UTF-8).
Вход, cookie сессии и CSRF описаны в README.md. Проверка: .\check-api.ps1.

## Если БД уже была запущена старым compose.yaml
Скопируйте новые файлы в ТУ ЖЕ папку проекта, из которой запускали старый Compose.
Так сохранится имя Compose-проекта и его том coffee_data. Выполните ту же команду
`docker compose up --build -d`: Compose добавит приложение и использует данные БД.
Не меняйте имя Compose-проекта (-p) при обновлении. В новой папке с другим именем
Compose может создать отдельный том и новую тестовую БД.
Не удаляйте тома, чтобы сохранить заказы. Обычный docker compose down том не удаляет.
Если у существующей БД иной пароль, укажите именно его через .env.
Изменение POSTGRES_PASSWORD не меняет пароль уже инициализированного PostgreSQL.
При обновлении оставьте DB_INIT_MODE=always, чтобы применить migration.sql.
Значение never допустимо после ручного применения schema.sql и migration.sql.

## Порты заняты
Если на 5432 работает другая БД, создайте .env рядом с compose.yaml:

    APP_PORT=8081
    DB_PORT=5433
    POSTGRES_PASSWORD=postgres
    DB_INIT_MODE=always

После запуска API будет на http://localhost:8081/api/orders/1.
Внутри Docker приложение обращается к db:5432: внешние порты на это не влияют.
Если используете другой API-порт, исправьте его в requests.http/check-api.ps1.

## Полезные команды
    docker compose ps
    docker compose logs --tail=100 app
    docker compose logs --tail=100 db
    docker compose restart app
    docker compose down

После изменения Java-кода: docker compose up --build -d.
Для просмотра реальных записей PostgreSQL:

    docker compose exec db psql -U postgres -d coffee_shop -c "SELECT * FROM coffee_shop.orders ORDER BY id DESC;"

## Что находится в образе
Build stage: Maven + JDK 25, сборка с тестами.
Runtime stage: JRE 25 и JAR, запуск от отдельного пользователя app.
PostgreSQL: отдельный контейнер и постоянный том coffee_data.
Compose ждёт healthcheck БД перед запуском Java.
Приложение доступно на localhost; пароль postgres — учебное значение.

## Проверка сборки
Факт успешной сборки зависит от доступности Docker Engine и загрузки зависимостей.
Не считайте контейнер запущенным до успешного docker compose up и строки Started
в логах. Статус локальной проверки указан при передаче архива.

Проверка конфигурации: docker compose config --quiet.

