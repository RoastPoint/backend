Полный запуск приложения и PostgreSQL в Docker: см. README_DOCKER.md.
Команда: docker compose up --build -d

# RoastPoint Backend — сценарий 1

Учебный Maven-проект: Java 25, Spring Boot 3.5.16, Spring JDBC, PostgreSQL.
Сценарий 1: создание заказа с позициями и чтение заказа по номеру.
Модель: categories, menu_items, users, orders, order_items — как в ER-диаграмме.
Вместо Hibernate здесь Spring JDBC: SQL явно виден и его проще объяснить на защите.

## Открытие в IntelliJ IDEA
1. Распакуйте архив. File → Open → выберите pom.xml → Open as Project.
2. File → Project Structure → Project SDK → JDK 25 (при необходимости Download JDK).
3. Дождитесь загрузки Maven-зависимостей. Нужен доступ к Maven Central.
4. Подготовьте PostgreSQL одним из двух способов ниже.
5. Запустите main в RoastPointApplication.java зелёной кнопкой.
6. Сервер слушает http://localhost:8080. Главная страница не предусмотрена;
   для проверки откройте http://localhost:8080/api/orders/1.

## PostgreSQL: локальная установка
В pgAdmin выполните отдельно в подключении postgres:

    CREATE DATABASE coffee_shop;

В application.properties указаны значения по умолчанию: пользователь postgres,
пароль postgres, порт 5432. Замените их на свои через переменные окружения
Run → Edit Configurations → Environment variables:

    DB_USER=postgres;DB_PASSWORD=ваш_пароль

При первом запуске приложение само создаёт схему coffee_shop, 5 таблиц и
тестовые данные (10 заказов). Повторный запуск не очищает данные.
PostgreSQL должен быть установлен и запущен: JDBC подключает приложение к
серверу, но сам сервер PostgreSQL внутри JVM не запускается.
Инициализация предназначена для одного экземпляра учебного приложения.

## PostgreSQL: Docker (альтернатива локальной установке)
Если установлен и запущен Docker Desktop, из папки проекта выполните:

    docker compose up -d

Дождитесь готовности контейнера, затем запускайте Java-приложение.
Остановить БД: docker compose down. Данные остаются в именованном томе.
Если локальный PostgreSQL уже занимает 5432, используйте его или поменяйте
порт в compose.yaml и DB_URL. Одновременно оба варианта не нужны.

## Подключение ранее созданной БД
Структура совместима с предыдущим coffee_shop.sql. При наличии готовой схемы
укажите DB_INIT_MODE=never. Приложение не будет выполнять schema.sql/data.sql.
URL должен содержать currentSchema=coffee_shop:

    DB_URL=jdbc:postgresql://localhost:5432/coffee_shop?currentSchema=coffee_shop

## Проверка API
Откройте requests.http: в IDEA с поддержкой HTTP Client нажимайте ▶ у запроса.
Если HTTP Client недоступен, используйте Postman либо PowerShell:

    $body = '{"cashierId":1,"items":[{"menuItemId":3,"quantity":2},{"menuItemId":8,"quantity":1}]}'
    $order = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/orders -ContentType 'application/json' -Body $body
    $order | ConvertTo-Json -Depth 5
    Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($order.id)" | ConvertTo-Json -Depth 5

Создание возвращает 201, заголовок Location и JSON:

    {"id":11,"createdAt":"2026-10-22T10:30:00","cashierId":1,"status":"accepted",
     "items":[{"menuItemId":3,"name":"Капучино","quantity":2,"unitPrice":200.00,"lineTotal":400.00},
              {"menuItemId":8,"name":"Круассан","quantity":1,"unitPrice":160.00,"lineTotal":160.00}],
     "total":560.00}

Номер и время в примере условные; используйте id из реального ответа.
Цены устанавливает сервер из меню, клиент передаёт только id и количество.
Изменение текущей цены меню не меняет сохранённую цену заказа.

## Методы и ошибки
- POST /api/orders — создание, 201; GET /api/orders/{id} — чтение, 200.
- 400 — пустой список, некорректный JSON/id/количество, повторы, неверная роль.
- 404 — кассир, позиция или заказ отсутствует.
- 409 — недоступная позиция или нарушение ограничения БД.
- 503 — временная ошибка/недоступность БД во время запроса.
- 500 — непредвиденная внутренняя ошибка; SQL и пароль в ответ не попадают.
Если БД недоступна при старте, приложение не запустится: проверьте сервер и пароль.
Пример ошибки: {"timestamp":"...","status":404,"message":"Заказ не найден: 999999","details":[]}.

## Как работает код
Controller принимает JSON и проверяет Bean Validation → Service проверяет кассира
и меню, создаёт заказ со строками через JdbcClient → PostgreSQL сохраняет данные.
@Transactional откатывает весь заказ при ошибке. BigDecimal используется для денег.
FOR SHARE защищает выбранные цены от изменения до завершения создания заказа.
GET собирает строки заказа и считает сумму quantity × unit_price.
OrderDto содержит входные/выходные record, ErrorHandler формирует единый ответ ошибок.

Это учебный MVP без входа по паролю и разграничения доступа HTTP. cashierId
передаётся явно; проверка роли не заменяет аутентификацию. password_hash содержит
демонстрационные маркеры, endpoint входа отсутствует. Оплата и остатки не реализованы.
Все 5 таблиц соответствуют ER-модели, отдельная таблица итогов не нужна.
Название позиции при чтении берётся из меню, цена — из строки заказа.

## Сборка и тесты
В окне Maven IDEA: Lifecycle → test, затем package.
Либо при установленном Maven/JDK:

    mvn test
    mvn package
    java -jar target/roastpoint-backend-1.0.0.jar

Controller-тесты не требуют PostgreSQL; они проверяют HTTP-контракт и валидацию.
Для проверки реального сохранения выполните POST, затем GET через requests.http,
перезапустите приложение и повторите GET: сохранённый заказ должен остаться.

Автоматическая проверка работающего API в PowerShell: .\check-api.ps1.
Она создаёт один новый заказ; запускать на тестовых данных с исходными ценами.

Статус проверки при подготовке: Maven был запущен, но загрузка зависимостей
из Maven Central заблокирована средой (Permission denied: getsockopt).
Поэтому успешная компиляция, тесты и соединение с реальной PostgreSQL здесь
не подтверждены. При первом открытии выполните Maven test и check-api.ps1.

