# SQL DAL Maker

### About

SQL DAL Maker is a generator of DTO, Model, and DAO classes to access relational databases. Target
programming languages: PHP, Java, C++, Python, and Go. To generate the class, you declare it in XML meta-program.

At the stage of code generation, the target database must exist and be available for JDBC connection.
Generated code is being synchronized with the current database schema using "live" JDBC metadata.

Implemented in Java as plug-ins for [Eclipse IDE](http://marketplace.eclipse.org/content/sql-dal-maker) and
[IntelliJ-Platform](http://plugins.jetbrains.com/plugin/7092).

Project Docs: [https://sqldalmaker.sourceforge.net](https://sqldalmaker.sourceforge.net/)

Quick Start in
mp4: [https://github.com/panedrone/sqldalmaker/releases/tag/latest](https://github.com/panedrone/sqldalmaker/releases/tag/latest)

### Hello Example

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

### Demo-Projects

<table>
<tr>
    <th>
        PHP
    </th>
    <th>
        Java
    </th>
    <th>
        C++
    </th>
    <th>
        Python
    </th>
    <th>
        Go
    </th>
</tr>
<tr>
    <td>
        <ul>
        <li><a href="https://github.com/panedrone/sdm_demo_php_todolist">PDO</a></li>
        <li><a href="https://github.com/panedrone/sdm_demo_todolist_php_doctrine">Doctrine</a></li>
      </ul>
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
        <a href="https://github.com/panedrone/sdm_demo_qt6_thesaurus">QtSql</a>
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
        <a href="https://github.com/panedrone/sdm_todolist_golang_react_js">database/sql, sqlx, gorm</a>
    </td>
</tr>
<tr>
    <td>
        React.js
    </td>
    <td>
    </td>
    <td>
        Qt
    </td>
    <td>
        Vue.js
    </td>
    <td>
        React.js
    </td>
</tr>
</table>

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
        <a href="https://github.com/panedrone/sdm_todolist_golang_react_js">database/sql + sqlx + gorm</a>
    </td>
    <td>
        React.js
    </td>
</tr>
</table>