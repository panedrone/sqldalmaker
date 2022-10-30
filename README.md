# SQL DAL Maker
SQL DAL Maker is a generator of DTO, Model, and DAO classes to access relational databases. Target programming languages: PHP, Java, C++, Python, Ruby, and Go. To generate the class, you declare it in XML meta-program.

At the stage of code generation, the target database must exist and be available for JDBC connection.
"Live" JDBC metadata are used to synchronize generated code with the current database schema.

![SQL DAL Maker](sdm_dj-sa.png)

Implemented as plug-ins for [Eclipse IDE](http://marketplace.eclipse.org/content/sql-dal-maker), [IntelliJ-Platform](http://plugins.jetbrains.com/plugin/7092), and [NetBeans 11+](https://github.com/panedrone/sqldalmaker/releases/tag/latest).

Project Website: [http://sqldalmaker.sourceforge.net/](http://sqldalmaker.sourceforge.net/)

Quick-Demo in mp4: [https://github.com/panedrone/sqldalmaker/releases/tag/latest](https://github.com/panedrone/sqldalmaker/releases/tag/latest)

|              | Demo-Projects |
| ------------ | ----------- |
| PHP          | [PDO](https://github.com/panedrone/sdm_demo_php_todolist), [Doctrine](https://github.com/panedrone/sdm_demo_todolist_php_doctrine) |
| Java JDBC    | [Swing](https://github.com/panedrone/sdm_demo_swing_thesaurus), [JSF](https://github.com/panedrone/sdm_demo_jsf_todolist), [JasperReports](https://github.com/panedrone/sdm_demo_jasper_reports_northwindEF) |
| Java Android | [Android](https://github.com/panedrone/sdm_demo_android_thesaurus) |
| C++          | [Qt](https://github.com/panedrone/sdm_demo_qt6_thesaurus)
| Python       | [Django](https://github.com/panedrone/sdm_demo_django_todolist_sqlite3), [Flask-SQLAlchemy](https://github.com/panedrone/sdm_demo_flask_sqlalchemy_todolist), [sqlite3](https://github.com/panedrone/sdm_demo_python_tkinter_github_stat) |
| Go           | [database/sql](https://github.com/panedrone/sdm_demo_go_todolist), [Gorm](https://github.com/panedrone/sdm_demo_gorm_todolist) |