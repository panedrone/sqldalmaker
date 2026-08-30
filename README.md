# SQL DAL Maker

### About

SQL DAL Maker generates the data access layer — DTO, Model, and DAO classes — for PHP, Java, C++, Python, and Go.

It works in three steps:

1. **Reverse-engineer.** The generator connects to your database over JDBC and reads live metadata: tables, views, and
   the result sets of your own SQL. The database must be up and reachable while you generate.
2. **Fine-tune.** What it found is written out as an XML manifest. You override the types your driver got wrong, add
   fields it could not see, and pick which CRUD methods you actually need.
3. **Translate.** The manifest is translated into complete source code — free of any direct call to PDO, JDBC, or QtSql.
   Those stay behind a `DataStore` interface that you own.

The manifest persists, so regeneration is repeatable: when the schema changes, you re-run the generator and your
corrections are still there.

Implemented in Java as plug-ins for [Eclipse IDE](http://marketplace.eclipse.org/content/sql-dal-maker) and
[IntelliJ Platform](http://plugins.jetbrains.com/plugin/7092).

Project Docs: [https://sqldalmaker.sourceforge.net](https://sqldalmaker.sourceforge.net/)

Quick Start in
mp4: [https://github.com/panedrone/sqldalmaker/releases/tag/latest](https://github.com/panedrone/sqldalmaker/releases/tag/latest)

### Hello-App

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
def generated_code_in_action():
    ds = scoped_ds()
    dao = MessagesDao(ds)
    m = Message()
    m.text = "Hello, World!"
    dao.create_message(m)
    print(m.id) # new "id" is available now
    m.text = "Hello, SDM World!"
    dao.update_message(m)
    for msg in dao.get_messages_like("hello%"):
        print(msg)
    dao.delete_message(m.id)
```

### Install and Update

**Eclipse IDE 3.8+**

Follow this: [https://marketplace.eclipse.org/content/sql-dal-maker/help](https://marketplace.eclipse.org/content/sql-dal-maker/help)
or use drag-and-drop feature from here: [https://marketplace.eclipse.org/content/sql-dal-maker](https://marketplace.eclipse.org/content/sql-dal-maker)

Update site URL 1 (fast access): https://sourceforge.net/projects/sqldalmaker/files/eclipse

Update site URL 2 (redirected): https://sqldalmaker.sourceforge.net/eclipse

**IntelliJ Platform 222.3345.118+**

Supported products: PhpStorm, IntelliJ IDEA, Android Studio, CLion, PyCharm, GoLand.

IDE Menu "File" > Settings > Plugins > Marketplace > SQL DAL Maker > Install.

IDE Menu "File" > Settings > Plugins > Installed > SQL DAL Maker > Update.

Or use "Install..." button from the plug-in web-page: [http://plugins.jetbrains.com/plugin/7092](http://plugins.jetbrains.com/plugin/7092)

### Demo Projects

<table>
<tr>
    <td>
        PHP
    </td>
    <td>
      <ul>
        <li><a href="https://github.com/panedrone/sdm_demo_php_todolist">PDO</a></li>
        <li><a href="https://github.com/panedrone/sdm_demo_todolist_php_doctrine">Doctrine</a></li>
      </ul>
    </td>
    <td>
        React.js
    </td>
</tr>
<tr>
    <td>
        Java
    </td>
    <td>
      <ul>
        <li><a href="https://github.com/panedrone/sdm_demo_jsf_todolist">JDBC + JSF</a></li>
        <li><a href="https://github.com/panedrone/sdm_demo_java_jdbc_swing_thesaurus_sqlite3">JDBC + Swing</a></li>
        <li><a href="https://github.com/panedrone/sdm_demo_jasper_reports_northwindEF">JDBC + JasperReports</a></li>
        <li><a href="https://github.com/panedrone/sdm_demo_android_thesaurus">Android, SQLite3</a></li>
      </ul>
    </td>
    <td>
    </td>
</tr>
<tr>
    <td>
        C++
    </td>
    <td>
        <a href="https://github.com/panedrone/sdm_demo_qt6_thesaurus">QtSql</a>
    </td>
    <td>
        Qt
    </td>
</tr>
<tr>
    <td>
        Python
    </td>
    <td>
    <ul>
        <li><a href="https://github.com/panedrone/sdm_demo_todolist_flask_sqlalchemy">Flask + Flask-SQLAlchemy</a></li>
        <li><a href="https://github.com/panedrone/sdm_demo_todolist_fastapi_sqlalchemy">FastAPI + SQLAlchemy</a></li>
        <li><a href="https://github.com/panedrone/sdm_demo_fastapi_no_orm_scenario">FastAPI + no-orm-scenario</a></li>
        <li><a href="https://github.com/panedrone/sdm_demo_todolist_django">DRF + django.db</a></li>
    </ul>
    </td>
    <td>
        Vue.js
    </td>
</tr>
<tr>
    <td>
        Go
    </td>
    <td>
        database/sql + sqlx + gorm
    </td>
    <td>
        <a href="https://github.com/panedrone/sdm_todolist_go_react_vue_npm_sqlite3">React.js, Vue.js</a>        
    </td>
</tr>
</table>