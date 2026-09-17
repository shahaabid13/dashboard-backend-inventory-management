# SDNET Monitor setup

SDNET Monitor is embedded in the main Spring Boot application under
`com.sscl.sdnetmonitor`. Its persistence is isolated from the main application
database through `sdnet.datasource.*`.

Before startup, create the separate database and tables by running
`src/main/resources/db/sdnet-monitor/schema.sql` against MySQL. The embedded
entity manager uses `hibernate.hbm2ddl.auto=validate` and will not create or
modify the schema.

Set `SDNET_DB_PASSWORD` and, when needed, `SDNET_DB_URL` and
`SDNET_DB_USERNAME` in the environment before startup. The SDNET controllers
and persistence are loaded with the host application; the monitoring sweep is
separately controlled by `sdnet.monitoring.enabled`.

For a local MySQL instance, provision the configured account before starting
the application:

```sql
CREATE DATABASE IF NOT EXISTS sdnet_monitor
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'sdnet_app'@'localhost'
    IDENTIFIED BY '<choose-a-password>';

ALTER USER 'sdnet_app'@'localhost'
    IDENTIFIED BY '<choose-a-password>';

GRANT ALL PRIVILEGES ON sdnet_monitor.* TO 'sdnet_app'@'localhost';
FLUSH PRIVILEGES;
```

Then set the same password used above:

```powershell
$env:SDNET_DB_USERNAME = "sdnet_app"
$env:SDNET_DB_PASSWORD = "<choose-a-password>"
```

Run `src/main/resources/db/sdnet-monitor/schema.sql` against
`sdnet_monitor` before starting the application. An `Access denied for user
'sdnet_app'@'localhost'` error means these credentials do not match the
account configured in MySQL; the subsequent Hibernate dialect error is only a
consequence of that failed connection.

All `/api/sdnet-monitor/**` endpoints use the host application's existing JWT
and LDAP security and CORS configuration. The standalone SDNET application and
its permissive CORS configuration are intentionally not included.
