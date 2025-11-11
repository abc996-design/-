 # build_intro.py
import sqlite3
import requests
import json

conn = sqlite3.connect('database.db')
cur = conn.cursor()
cur.execute("SELECT title FROM movies ORDER BY title")
titles = [row[0] for row in cur.fetchall()]
conn.close()

intro_map = {}
placeholder = "暂无简介，欢迎补充。"

for title in titles:
    # 去掉末尾年份，提高匹配率
    clean = title.replace("'", "").split("(")[0].strip()
    try:
        # 免 key 公开接口，每天限额 40 IP/min 足够
        url = f"https://api.themoviedb.org/3/search/movie"
        params = {"query": clean, "language": "zh-CN"}
        res = requests.get(url, params=params, timeout=5)
        data = res.json()
        if data.get("results"):
            overview = data["results"][0].get("overview")
            if overview:
                intro_map[title] = overview
                continue
    except Exception:
        pass
    intro_map[title] = placeholder

# 打印成前端可直接用的格式
print("const introMap = {")
for k, v in intro_map.items():
    print(f"  '{k}': '{v.replace(chr(39), '\\'')}',\")  # 转义单引号
print("};")