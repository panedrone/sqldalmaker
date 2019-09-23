require 'dbi'

class DataStore

  #  SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
  #  About DataStore: http://sqldalmaker.sourceforge.net/how-to-start.html
  #  Contact: sqldalmaker@gmail.com

  #  This is an example of how to implement DataStore in Ruby + DBI.
  #  Copy-paste this code to your project and change it for your needs.

  def connect
    @dbi = DBI.connect('DBI:SQLite3:orders.sqlite')
  end

  def disconnect
    @dbi.disconnect
  end

  # TODO: add transaction management

  def insert_row(sql, ai_values=[], *params)
    @dbi.prepare(sql) do |sth|
      sth.execute(params)
    end
    if ai_values.length > 0
      # dbh.func(:insert_id) => Fixnum --- does not work with DBI:SQLite
      ai_values[0][1] = query_scalar_value('SELECT last_insert_rowid()')
    end
  end

  def exec_dml(sql, *params)
    @dbi.prepare(sql) do |sth|
      sth.execute(params)
    end
  end

  def query_scalar_value(sql, *params)
    res = nil
    @dbi.prepare(sql) do |sth|
      sth.execute(params)
      row = sth.fetch
      unless row.nil?
        res = row[0]
      end
    end
    res
  end

  def query_all_scalar_values(sql, *params)
    res = []
    @dbi.prepare(sql) do |sth|
      sth.execute(params)
      while !(row = sth.fetch).nil? do
        res << row[0]
      end
    end
    res
  end

  def query_first_row(sql, *params)
    res = nil
    @dbi.prepare(sql) do |sth|
      sth.execute(params)
      row = sth.fetch_hash
      unless row.nil?
        res = row
      end
    end
    res
  end

  def query_all_rows(sql, *params, &block)
    @dbi.prepare(sql) do |sth|
      sth.execute(params)
      while !(row = sth.fetch_hash).nil? do
        block.call(row)
      end
    end
  end

end