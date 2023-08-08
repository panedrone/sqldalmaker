# SQL DAL Maker

SQL DAL Maker is a generator of DTO, Model, and DAO classes to access relational databases. Target
programming languages: PHP, Java, C++, Python, and Go. 

Written in Java. Implemented as plug-ins for [Eclipse IDE](http://marketplace.eclipse.org/content/sql-dal-maker),
[IntelliJ-Platform](http://plugins.jetbrains.com/plugin/7092),
and [NetBeans 11+](https://github.com/panedrone/sqldalmaker/releases/tag/latest).

At the stage of code generation, the target database must exist and be available for JDBC connection.
Generated code is being synchronized with the current database schema using "live" JDBC metadata.

To generate the class, you declare it in XML meta-program.

![SQL DAL Maker](sdm_python.png)

Project Website: [https://sqldalmaker.sourceforge.net](https://sqldalmaker.sourceforge.net/)

Quick Start in
mp4: [https://github.com/panedrone/sqldalmaker/releases/tag/latest](https://github.com/panedrone/sqldalmaker/releases/tag/latest)

|        | Demo Projects                                                                                                                                                                                                                                                                                                                                                                   |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| PHP    | [PDO + Vue.js](https://github.com/panedrone/sdm_demo_php_todolist), [Doctrine + Vue.js](https://github.com/panedrone/sdm_demo_todolist_php_doctrine)                                                                                                                                                                                                                            |
| Java   | [JDBC + JSF](https://github.com/panedrone/sdm_demo_jsf_todolist), [JDBC + JasperReports](https://github.com/panedrone/sdm_demo_jasper_reports_northwindEF), [Android](https://github.com/panedrone/sdm_demo_android_thesaurus)                                                                                                                                                  |
| C++    | [Qt](https://github.com/panedrone/sdm_demo_qt6_thesaurus)                                                                                                                                                                                                                                                                                                                       |
| Python | [Django + Vue.js](https://github.com/panedrone/sdm_demo_todolist_django)<br/>[FastAPI/SQLAlchemy + Vue.js](https://github.com/panedrone/sdm_demo_todolist_fastapi_sqlalchemy)<br/>[FastAPI/no-ORM-scenario + Vue.js](https://github.com/panedrone/sdm_demo_fastapi_sqlalchemy)<br/>[Flask-SQLAlchemy + Vue.js](https://github.com/panedrone/sdm_demo_todolist_flask_sqlalchemy) |
| Go     | [no-ORM-scenario/Gorm + Vue.js](https://github.com/panedrone/sdm_demo_todolist_sqlite3_golang)                                                                                                                                                                                                                                                                                  |