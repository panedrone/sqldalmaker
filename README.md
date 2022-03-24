# SQL DAL Maker
SQL DAL Maker is a generator of DTO and DAO classes to access relational databases. Target programming languages: PHP, Java, C++, Python, Ruby, and Go. To generate the class, you declare it in XML meta-program.

At the stage of code generation, the target database must exist and be available for JDBC connection.
Code generator uses "live" JDBC metadata to make generated code relevant to the current database schema. 

Implemented as plug-ins for Eclipse IDE, IntelliJ-Platform, and NetBeans 11+.

All the details are here: [http://sqldalmaker.sourceforge.net/](http://sqldalmaker.sourceforge.net/)

Quick-Demo in mp4: [https://github.com/panedrone/sqldalmaker/releases/tag/latest](https://github.com/panedrone/sqldalmaker/releases/tag/latest)

Demo-Projects:
* [PHP/PDO/REST](https://github.com/panedrone/sdm_demo_php_todolist) 
* [Java/JDBC/Swing](https://github.com/panedrone/sdm_demo_swing_thesaurus)
* [Java/JDBC/JSF](https://github.com/panedrone/sdm_demo_jsf_todolist)
* [Java/JDBC/Jasper-Reports](https://github.com/panedrone/sdm_demo_jasper_reports_northwindEF)
* [Java/Android](https://github.com/panedrone/sdm_demo_android_thesaurus)
* [C++/Qt6](https://github.com/panedrone/sdm_demo_qt6_thesaurus)
* [Python/Flask-RESTful](https://github.com/panedrone/sdm_demo_python_flask_todolist)
* [Python/Tkinter](https://github.com/panedrone/sdm_demo_python_tkinter_github_stat)
* [Go/Gin](https://github.com/panedrone/sdm_demo_go_todolist)

[SQL DAL Maker](sqldalmaker-idea.png)
