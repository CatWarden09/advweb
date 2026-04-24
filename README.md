Проект Advweb - это онлайн-сервис объявлений, который состоит из бекенда и фронтенда, размещен на
реальном сервере (VPS) и имеет реальный сайт. Проект демонстрирует практические навыки, полученные в рамках курса по backend-разработке.


## Ключевые особенности проекта
- Система регистрации и авторизации пользователей с распределением ролей через Keycloak
- Подтверждение email при регистрации и возможность восстановления пароля для пользователей
- Система модерации с ограниченным доступ с ролью Администратора (объявления, комментарии, отзывы,
категории объявлений)
- Эндпоинты для управления категориями объявлений через UI
- Отзывы о пользователях с системой рейтинга
- Добавление объявлений в избранное
- Пользовательские аватарки
- Автоматическая очистка неиспользуемых файлов на стороне сервера (фото, аватарки)
- Подсчет просмотров объявлений
- Подсчет общей суммы продаж пользователя при снятии объявления с публикации
- Автоматическая еженедельная рассылка пользователям с топом просмотров объявлений
- Пагинация данных для фронтенда

## Стек проекта
- Java 21
- Spring Boot
- PostgreSQL
- Flyway
- QueryDSL
- Keycloak
- Redis
- Kafka
- Maven
- JUnut5
- Mockito
- JaCoCo

## Стек деплоймента на сервере (VPS)
- Ubuntu 24.04
- DuckDNS
- Let's Encrypt
- Nginx
- Kubernetes (k3s) + Headlamp

## Зависимости на стороне сервера
- Kubectl
- Nginx с настроенным конфигом
- Git
- Домен и SSL-сертификат

## Первый запуск проекта

1. Клонировать репозиторий и перейти в проект:

```bash
git clone https://github.com/CatWarden09/advweb.git
cd advweb
```

2. Создать namespace (если еще не создан):

```bash
kubectl apply -f k8s/adweb-namespace.yaml
```

3. Создать секрет `advweb-env` (используется `app`, `db`, `keycloak`):

```bash
kubectl -n advweb create secret generic advweb-env \
  --from-literal=POSTGRES_DB=advweb \
  --from-literal=POSTGRES_USER=advweb_user \
  --from-literal=POSTGRES_PASSWORD=change_me \
  --from-literal=KEYCLOAK_ADMIN_USERNAME=admin \
  --from-literal=KEYCLOAK_ADMIN_PASSWORD=change_me \
  --from-literal=APP_PUBLIC_BASE_URL=https://advweb.duckdns.org \
  --from-literal=APP_KEYCLOAK_BASE_URL=https://advweb.duckdns.org/auth \
  --from-literal=SPRING_MAIL_USERNAME=your_mail@gmail.com \
  --from-literal=SPRING_MAIL_PASSWORD=your_app_password
```


4. Применить манифесты:

```bash
kubectl apply -f k8s/
```

5. Проверить, что поды запустились:

```bash
kubectl get pods -n advweb
```

6. Настроить Keycloak (минимум для первого входа):
- Зайти в admin-консоль Keycloak.
- Создать realm `advweb` (если отсутствует).
- Создать роли realm-уровня: `USER`, `ADMIN`.
- Создать client для фронта/swagger

7. Проверить доступность:
- API/Swagger: `https://advweb.duckdns.org/swagger-ui/index.html`



## Остановка/повторный запуск

`kubectl scale deployment --all --replicas=0 -n advweb`

`kubectl scale deployment --all --replicas=1 -n advweb`

## Доступ к дашборду Headlamp

### На сервере:

`kubectl -n headlamp port-forward svc/headlamp 8100:80 --address 127.0.0.1`

Получить токен при необходимости:

`kubectl -n headlamp create token headlamp-admin`

### На локальной машине:

1. `ssh -L 8100:127.0.0.1:8100 root@SERVER_IP`

2. Открыть в браузере http://localhost:8100
