from flask import Flask, request, render_template
import sqlite3

app = Flask(__name__)

def get_conn():
    return sqlite3.connect('database.db')

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/api/movies')
def api_movies():
    keyword = request.args.get('kw', '').strip()
    conn = get_conn()
    cur = conn.cursor()
    if keyword:
        sql = """
            SELECT m.title, AVG(r.rating) AS avg_score
            FROM movies m
            JOIN ratings r ON m.movieId = r.movieId
            WHERE m.title LIKE ?
            GROUP BY m.movieId, m.title
            ORDER BY avg_score DESC
            LIMIT 50
        """
        cur.execute(sql, ('%' + keyword + '%',))
    else:
        sql = """
            SELECT m.title, AVG(r.rating) AS avg_score
            FROM movies m
            JOIN ratings r ON m.movieId = r.movieId
            GROUP BY m.movieId, m.title
            ORDER BY avg_score DESC
            LIMIT 20
        """
        cur.execute(sql)
    rows = cur.fetchall()
    conn.close()

    # 假介绍库，你可以随时加/改
    intro_map = {
        'Toy Story': '皮克斯首部电脑动画，玩具们的奇妙冒险。',
        'Jumanji': '棋盘游戏一旦开始，现实与奇幻交织。',
        'Grumpier Old Men': '两位老顽童的爆笑晚年爱情故事。',
        'Waiting to Exhale': '四位黑人女性携手面对感情风浪。',
        'Father of the Bride Part II': '岳父大人同时迎来新生儿与孙子。',
        'Heat': '阿尔·帕西诺与德尼罗的经典警匪对决。',
        'Sabrina': '经典爱情童话的现代翻拍。',
        'Tom and Huck': '汤姆·索亚与哈克的童年探险。',
        'Sudden Death': '消防员单枪匹马拯救冰球场人质。',
        'GoldenEye': '皮尔斯·布鲁斯南首演007，重启系列。',
        'The American President': '白宫里的浪漫爱情与政治博弈。',
        'Dracula: Dead and Loving It': '恶搞版德古拉，笑点密集。',
        'Balto': '半狼半狗的英雄雪橇犬真实改编。',
        'Nixon': '奥斯卡级传记片，透视总统水门事件。',
        'Cutthroat Island': '海盗夺宝传奇，海战火爆。',
        'Casino': '马丁·斯科塞斯执导，拉斯维加斯黑帮兴衰。',
        'Sense and Sensibility': '李安执导，简·奥斯汀经典改编。',
        'Four Rooms': '四个荒诞故事拼成的酒店一夜。',
        'Ace Ventura: When Nature Calls': '金·凯瑞爆笑动物侦探再出击。',
        'Money Train': '兄弟搭档抢地铁运钞车动作大片。'
    }

    movies = [
        {
            'title': r[0],
            'score': round(r[1], 2),
            'intro': intro_map.get(r[0], '暂无简介，欢迎补充。')
        }
        for r in rows
    ]
    return {'movies': movies}

if __name__ == '__main__':
    app.run(debug=True)