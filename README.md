# SQL DAL Maker

SQL DAL Maker is a generator of DTO, Model, and DAO classes to access relational databases. Target
programming languages: PHP, Java, C++, Python, and Go. To generate the class, you declare it in XML meta-program. 

At the stage of code generation, the target database must exist and be available for JDBC connection.
Generated code is being synchronized with the current database schema using "live" JDBC metadata.

Hello Example:


```xml
<sdm>

    <dto-class name="Message" ref="messages"/>

    <dao-class name="MessagesDao">
        <crud dto="Message"/>
        <query-list method="get_messages_like(key)" ref="get_messages_like.sql"/>
    </dao-class>

</sdm>
```

```python
ds = create_ds()
dao = MessagesDao(ds)

m = Message()
m.text = "Hello"
dao.create_message(m)
print(m.id)

m.text = "Hello SDM!"
dao.update_message(m)

for msg in dao.get_messages_like("hello%"):
    print(msg)

dao.delete_message(m.id)
```

Implemented in Java as plug-ins for [Eclipse IDE](http://marketplace.eclipse.org/content/sql-dal-maker) and
[IntelliJ-Platform](http://plugins.jetbrains.com/plugin/7092).

Project Web-Site/Docs: [https://sqldalmaker.sourceforge.net](https://sqldalmaker.sourceforge.net/)

Quick Start in
mp4: [https://github.com/panedrone/sqldalmaker/releases/tag/latest](https://github.com/panedrone/sqldalmaker/releases/tag/latest)

|        | Demo Projects                                                                                                                                                                                                                                                                                                                                                                                                             | GUI      |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| PHP    | <ul><li>[PDO](https://github.com/panedrone/sdm_demo_php_todolist)</li><li>[Doctrine](https://github.com/panedrone/sdm_demo_todolist_php_doctrine)</li></ul>                                                                                                                                                                                                                                                               | Vue.js   |
| Java   | <ul><li>[JDBC + JSF](https://github.com/panedrone/sdm_demo_jsf_todolist)</li><li>[JDBC + Swing](https://github.com/panedrone/sdm_demo_java_jdbc_swing_thesaurus_sqlite3)</li><li>[JDBC + JasperReports](https://github.com/panedrone/sdm_demo_jasper_reports_northwindEF)</li><li>[Android, SQLite3](https://github.com/panedrone/sdm_demo_android_thesaurus)</li><ul>                                                    |          |
| C++    | [QtSql](https://github.com/panedrone/sdm_demo_qt6_thesaurus)                                                                                                                                                                                                                                                                                                                                                              | Qt       |
| Python | <ul><li>[DRF/django.db](https://github.com/panedrone/sdm_demo_todolist_django)</li><li>[FastAPI/SQLAlchemy](https://github.com/panedrone/sdm_demo_todolist_fastapi_sqlalchemy)</li><li>[FastAPI/no-ORM-scenario](https://github.com/panedrone/sdm_demo_fastapi_no_orm_scenario)</li><li>[Flask-SQLAlchemy](https://github.com/panedrone/sdm_demo_todolist_flask_sqlalchemy)</li></ul> | Vue.js   |
| Go     | [database/sql + sqlx + gorm](https://github.com/panedrone/sdm_golang_todolist_vue_spa)                                                                                                                                                                                                                                                                                                                                    | React.js |
