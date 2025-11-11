import pandas as pd
import sqlite3

conn = sqlite3.connect('database.db')

movies = pd.read_csv('movies.csv')
ratings = pd.read_csv('ratings.csv')

movies.to_sql('movies', conn, index=False, if_exists='replace')
ratings.to_sql('ratings', conn, index=False, if_exists='replace')

conn.close()
print("✅ 数据导入完成")